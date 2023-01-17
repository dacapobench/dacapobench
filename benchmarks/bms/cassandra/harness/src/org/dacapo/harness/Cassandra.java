/*
 * Copyright (c) 2009-2020 The Australian National University.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License v2.0.
 * You may obtain the license at
 * 
 *    http://www.opensource.org/licenses/apache2.0.php
 */
package org.dacapo.harness;

import java.io.*;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import sun.misc.Unsafe;
import java.lang.reflect.Field;

import org.dacapo.parser.Config;


public class Cassandra extends Benchmark {
    String[] args;

    private File dirLibSigar;
    private File dirCassandraConf;
    private File dirCassandraStorage;
    private File dirCassandraLog;
    private File dirYCSBWorkloads;
    private File ymlConf;
    private File xmlLogback;
    private Object cassandra;
    private Class<?> EmbeddedCassandraServiceClass;
    private String[] ycsbWorkloadArgs;

    private PrintStream outStream = System.out;
    private final PrintStream logStream = new PrintStream(new OutputStream() { @Override public void write(int b) throws IOException {/* Doing nothing */ } } );

    private Class<?> clsYCSBClient;
    private Method mtdYCSBClientMain;

    public Cassandra(Config config, File scratch, File data) throws Exception {
        super(config, scratch, data, false);
        assertJavaVersionLE(14, "Cassandra currently requires Java < 15.  See https://issues.apache.org/jira/browse/CASSANDRA-16895.");
    }


    private void setupData() {
        String path =  "dat"+File.separator+"cassandra"+File.separator;
        dirLibSigar = new File(data, path+"libsigar");
        dirCassandraConf = new File(data, path+"conf");
        dirYCSBWorkloads = new File(data, path+"ycsb");
        ymlConf = new File(dirCassandraConf, "cassandra.yaml");
        xmlLogback = new File(dirCassandraConf, "logback.xml");
    }

    private void setupScratch() {
        dirCassandraStorage = new File(scratch, "cassandra-storage");
        dirCassandraLog = new File(scratch, "cassandra-log");
        dirCassandraStorage.mkdir();
        dirCassandraLog.mkdir();

    }

    private void setupCassandra() {
        try {
            System.setProperty("java.library.path", dirLibSigar.getPath());
            System.setProperty("cassandra.storagedir", dirCassandraStorage.toString());
            System.setProperty("cassandra.logdir", dirCassandraLog.toString());
            System.setProperty("cassandra.config", ymlConf.toPath().toUri().toString());
            System.setProperty("cassandra.logback.configurationFile", xmlLogback.toString());
            System.setProperty("cassandra-foreground", "yes");

            EmbeddedCassandraServiceClass = Class.forName("org.apache.cassandra.service.EmbeddedCassandraService", true, loader);
            cassandra = EmbeddedCassandraServiceClass.getConstructor().newInstance();
        } catch (Exception e) {
            System.err.println("Exception during initialization: " + e.toString());
            e.printStackTrace();
            System.exit(-1);
        }
    }

    @Override
    protected void prepare(String size) throws Exception {
        super.prepare(size);
        args = config.preprocessArgs(size, scratch, data);

        /*
         *  FIXME
         * 
         *  This workaround silences JDK11 warnings relating to use of
         *  reflection.
         *
         *  Specifically, cassandra generates the following warning:
         * 
         *  WARNING: Illegal reflective access by org.apache.cassandra.utils.FBUtilities (file:/[...]/cassandra/cassandra-3.11.6.jar) to field java.io.FileDescriptor.fd
         *
         * Fixing the underlying issue means changing the upstream library,
         * which is beyond the scope of this benchmarking suite.
         */
        try {
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            Unsafe u = (Unsafe) theUnsafe.get(null);
      
            Class cls = Class.forName("jdk.internal.module.IllegalAccessLogger");
            Field logger = cls.getDeclaredField("logger");
            u.putObjectVolatile(cls, u.staticFieldOffset(logger), null);
        } catch (Exception e) {
            // ignore
        }

        setupData();
        setupScratch();

        setupCassandra();

        // Avoiding the long output of cassandra starting process
        System.setOut(logStream);

        outStream.println("Cassandra starting...");
        Method startMethod = EmbeddedCassandraServiceClass.getMethod("start");
        startMethod.invoke(cassandra);

        clsYCSBClient = loader.loadClass("site.ycsb.Client");
        mtdYCSBClientMain = clsYCSBClient.getMethod("main", String[].class);

        prepareYCSBArgs(size);

        prepareYCSBCQL();
    }

    private void prepareYCSBCQL() {
        outStream.println("YCSB starting...");

        try {
            Object sess;

            // Get the classes by reflection
            Class<?> cluster = Class.forName("com.datastax.driver.core.Cluster", true, loader);
            Class<?> clusterBuilder = Class.forName("com.datastax.driver.core.Cluster$Builder", true, loader);
            Class<?> DatabaseDescriptor = Class.forName("org.apache.cassandra.config.DatabaseDescriptor", true, loader);
            Class<?> session = Class.forName("com.datastax.driver.core.Session", true, loader);

            // Get the methods by reflection
            Method getNativeTransportPort = DatabaseDescriptor.getMethod("getNativeTransportPort");
            Method builder = cluster.getMethod("builder");
            Method connect = cluster.getDeclaredMethod("connect");

            Method withPort = clusterBuilder.getDeclaredMethod("withPort", int.class);
            Method build = clusterBuilder.getDeclaredMethod("build");
            Method addContactPoint = clusterBuilder.getDeclaredMethod("addContactPoint", String.class);

            Method execute =  session.getDeclaredMethod("execute", String.class);
            Method close =  session.getDeclaredMethod("close");

            // Get the mid level result
            Object tr = builder.invoke(null);

            // Obtain the non-static method by reflection
            tr = addContactPoint.invoke(tr, "localhost");

            // Get the port number
            tr = withPort.invoke(tr, (Object) getNativeTransportPort.invoke(null));
            // Get the session
            sess = connect.invoke(build.invoke(tr));

            execute.invoke(sess, (Object) "CREATE KEYSPACE ycsb WITH REPLICATION = {'class': 'SimpleStrategy', 'replication_factor': 1};");
            execute.invoke(sess, (Object) "USE ycsb;");
            execute.invoke(sess, (Object) "CREATE TABLE usertable (" +
                                                 "y_id varchar primary key," +
                                                 "field0 varchar," +
                                                 "field1 varchar," +
                                                 "field2 varchar," +
                                                 "field3 varchar," +
                                                 "field4 varchar," +
                                                 "field5 varchar," +
                                                 "field6 varchar," +
                                                 "field7 varchar," +
                                                 "field8 varchar," +
                                                 "field9 varchar);");
            // Close the session
            close.invoke(sess);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void prepareYCSBArgs(String size) {
        ArrayList<String> baseArgs = new ArrayList<String>(Arrays.asList(
            "-db", "site.ycsb.db.CassandraCQLClient", 
            "-threads", Integer.toString(config.getThreadCount(size)),
            "-p", "hosts=localhost"
            ));
        List<String> sizeArgs = Arrays.asList(args);
        File workload = new File(dirYCSBWorkloads, sizeArgs.get(0));
        baseArgs.addAll(Arrays.asList("-P", workload.toString()));
        baseArgs.addAll(sizeArgs.subList(1, sizeArgs.size()));
        baseArgs.add(baseArgs.size(), "-t");
        ycsbWorkloadArgs = baseArgs.toArray(new String[0]);
    }

    public void iterate(String size) throws Exception {
        System.setOut(logStream);

        // load workload
        ycsbWorkloadArgs[ycsbWorkloadArgs.length - 1] = "-load";
        mtdYCSBClientMain.invoke(null, (Object)ycsbWorkloadArgs);
        
        // run transactions
        ycsbWorkloadArgs[ycsbWorkloadArgs.length - 1] = "-t";
        mtdYCSBClientMain.invoke(null, (Object)ycsbWorkloadArgs);
    }

    @Override
    public void postIteration(String size) throws Exception {
        super.postIteration(size);
        //Preventing the long stopping log information from cassandra
        System.setOut(logStream);
    }

    public void cleanup() {
        // Clean the scratch up
        if (!getPreserve()) {
            deleteTree(dirCassandraStorage);
            deleteTree(dirCassandraLog);
        }
    }
}
