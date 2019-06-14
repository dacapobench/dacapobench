/*
 * Copyright (c) 2009 The Australian National University.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License v2.0.
 * You may obtain the license at
 * 
 *    http://www.opensource.org/licenses/apache2.0.php
 */
package org.dacapo.harness;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.dacapo.parser.Config;

import org.apache.cassandra.config.DatabaseDescriptor;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;

public class CassandraBench extends Benchmark {

    private String[] YCSB_JARS = {
        "cassandra-binding-0.15.0.jar",
        "cassandra-driver-core-3.0.0.jar",
        "guava-16.0.1.jar",
        "metrics-core-3.1.2.jar",
        "netty-buffer-4.0.33.Final.jar",
        "netty-codec-4.0.33.Final.jar",
        "netty-common-4.0.33.Final.jar",
        "netty-handler-4.0.33.Final.jar",
        "netty-transport-4.0.33.Final.jar",
        "slf4j-api-1.7.25.jar",
        "slf4j-simple-1.7.25.jar",
        "core-0.15.0.jar",
        "HdrHistogram-2.1.4.jar",
        "htrace-core4-4.1.0-incubating.jar",
        "jackson-core-asl-1.9.4.jar",
        "jackson-mapper-asl-1.9.4.jar"
    };

    private ClassLoader clCassandra;
    private ClassLoader clYCSB;
    private ClassLoader clOriginal;

    private File dirScratchJar;
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

    private PrintStream outStream;
    private final PrintStream logStream = new PrintStream(new FileOutputStream(new File(scratch.toPath() + File.separator + "stdout.log")));
    private final PrintStream errStream = new PrintStream(new FileOutputStream(new File(scratch.toPath() + File.separator + "stderr.log")));

    Class<?> clsYCSBClient;
    Method mtdYCSBClientMain;

    public CassandraBench(Config config, File scratch, File data) throws Exception {
        super(config, scratch, data, false);
    }

    private void setupScratch() {
        dirScratchJar = new File(scratch, "jar");
        dirLibSigar = new File(scratch, "libsigar");
        dirCassandraConf = new File(scratch, "cassandra-conf");
        dirCassandraStorage = new File(scratch, "cassandra-storage");
        dirCassandraLog = new File(scratch, "cassandra-log");
        dirYCSBWorkloads = new File(scratch, "ycsb-workloads");

        ymlConf = new File(dirCassandraConf, "cassandra.yaml");
        xmlLogback = new File(dirCassandraConf, "logback.xml");

        dirLibSigar.mkdir();
        dirCassandraConf.mkdir();
        dirCassandraStorage.mkdir();
        dirCassandraLog.mkdir();
        dirYCSBWorkloads.mkdir();
        try {
            Benchmark.unpackZipFileResource("dat/libsigar.zip", dirLibSigar);
            Benchmark.unpackZipFileResource("dat/cassandra-conf.zip", dirCassandraConf);
            Benchmark.unpackZipFileResource("dat/ycsb-workloads.zip", dirYCSBWorkloads);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }
    private void setupCassandra() {
        try {
            System.setProperty("java.library.path", dirLibSigar.getPath());
            System.setProperty("cassandra.storagedir", dirCassandraStorage.toString());
            System.setProperty("cassandra.logdir", dirCassandraLog.toString());
            System.setProperty("cassandra.config", ymlConf.toURI().toString());
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
//        String[] cassandraArgs = config.getArgs(size);
        clOriginal = Thread.currentThread().getContextClassLoader();

        setupScratch();

        setupCassandra();

        // Avoiding the long output of cassandra starting process
        outStream = System.out;
        System.setOut(logStream);

        outStream.println("Cassandra starting...");
        Method startMethod = EmbeddedCassandraServiceClass.getMethod("start");
        startMethod.invoke(cassandra);

        clsYCSBClient = loader.loadClass("com.yahoo.ycsb.Client");
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

//        Session session = cluster.connect();
//        session.execute("CREATE KEYSPACE ycsb WITH REPLICATION = {'class': 'SimpleStrategy', 'replication_factor': 1};");
//        session.execute("USE ycsb;");
//        session.execute("CREATE TABLE usertable (" +
//                            "y_id varchar primary key," +
//                            "field0 varchar," +
//                            "field1 varchar," +
//                            "field2 varchar," +
//                            "field3 varchar," +
//                            "field4 varchar," +
//                            "field5 varchar," +
//                            "field6 varchar," +
//                            "field7 varchar," +
//                            "field8 varchar," +
//                            "field9 varchar);");
//        session.close();
    }

    private void prepareYCSBArgs(String size) {
        ArrayList<String> baseArgs = new ArrayList<String>(Arrays.asList(
            "-db", "com.yahoo.ycsb.db.CassandraCQLClient",
            "-p", "hosts=localhost"));
        List<String> sizeArgs = Arrays.asList(config.getArgs(size));
        File workload = new File(dirYCSBWorkloads, sizeArgs.get(0));
        baseArgs.addAll(Arrays.asList("-P", workload.toString()));
        baseArgs.addAll(sizeArgs.subList(1, sizeArgs.size()));
        baseArgs.add(baseArgs.size(), "-t");
        ycsbWorkloadArgs = baseArgs.toArray(new String[0]);
    }

    public void iterate(String size) throws Exception {
        System.setOut(logStream);
        outStream.println("DaCapo: start iteration");
        Thread.currentThread().setContextClassLoader(clYCSB);

        // load workload
        ycsbWorkloadArgs[ycsbWorkloadArgs.length - 1] = "-load";
        mtdYCSBClientMain.invoke(null, (Object)ycsbWorkloadArgs);
        
        // run transactions
        ycsbWorkloadArgs[ycsbWorkloadArgs.length - 1] = "-t";
        mtdYCSBClientMain.invoke(null, (Object)ycsbWorkloadArgs);
        outStream.println("DaCapo: finished iteration");
    }

    @Override
    public void postIteration(String size) throws Exception {
        super.postIteration(size);
        //Preventing the long stopping log information from cassandra
        System.setErr(errStream);
        System.setOut(logStream);
    }

    private static List<URL> findJars(File dir, List<String> jarNames) {
        try {
            return Files.walk(dir.toPath())
                    .filter(p -> p.endsWith(".jar") ||
                            (jarNames != null && jarNames.contains(p.getFileName().toString())))
                    .map(Path::toUri).map(uri -> {
                        try {
                            return uri.toURL();
                        } catch (MalformedURLException e) {
                            e.printStackTrace();
                            return null;
                        }
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
