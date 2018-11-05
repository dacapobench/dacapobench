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
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.cassandra.config.DatabaseDescriptor;
import org.dacapo.parser.Config;

import org.apache.cassandra.service.EmbeddedCassandraService;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;

public class CassandraBench extends Benchmark {
    private String [] CASSANDRA_JARS = {
        "cassandra-3.11.3.jar",
        "cassandra-thrift-3.11.3.jar",
        "HdrHistogram-2.1.9.jar",
        "ST4-4.0.8.jar",
        "airline-0.6.jar",
        "antlr-runtime-3.5.2.jar",
        "asm-5.0.4.jar",
        "caffeine-2.2.6.jar",
        "cassandra-driver-core-3.0.1-shaded.jar",
        "commons-cli-1.1.jar",
        "commons-codec-1.9.jar",
        "commons-lang3-3.1.jar",
        "commons-math3-3.2.jar",
        "compress-lzf-0.8.4.jar",
        "concurrent-trees-2.4.0.jar",
        "concurrentlinkedhashmap-lru-1.4.jar",
        "disruptor-3.0.1.jar",
        "ecj-4.4.2.jar",
        "guava-18.0.jar",
        "high-scale-lib-1.0.6.jar",
        "hppc-0.5.4.jar",
        "jackson-core-asl-1.9.13.jar",
        "jackson-mapper-asl-1.9.13.jar",
        "jamm-0.3.0.jar",
        "javax.inject.jar",
        "jbcrypt-0.3m.jar",
        "jcl-over-slf4j-1.7.7.jar",
        "jctools-core-1.2.1.jar",
        "jflex-1.6.0.jar",
        "jna-4.2.2.jar",
        "joda-time-2.4.jar",
        "json-simple-1.1.jar",
        "jstackjunit-0.0.1.jar",
        "libthrift-0.9.2.jar",
        "log4j-over-slf4j-1.7.7.jar",
        "logback-classic-1.1.3.jar",
        "logback-core-1.1.3.jar",
        "lz4-1.3.0.jar",
        "metrics-core-3.1.5.jar",
        "metrics-jvm-3.1.5.jar",
        "metrics-logback-3.1.5.jar",
        "netty-all-4.0.44.Final.jar",
        "ohc-core-0.4.4.jar",
        "ohc-core-j8-0.4.4.jar",
        "reporter-config-base-3.0.3.jar",
        "reporter-config3-3.0.3.jar",
        "sigar-1.6.4.jar",
        "slf4j-api-1.7.7.jar",
        "snakeyaml-1.11.jar",
        "snappy-java-1.1.1.7.jar",
        "snowball-stemmer-1.3.0.581.1.jar",
        "stream-2.5.2.jar",
        "thrift-server-0.3.7.jar",
        "cassandra-driver-core-3.6.0.jar"
    };

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
    private EmbeddedCassandraService cassandra;
    private String[] ycsbWorkloadArgs;

    Class<?> clsYCSBClient;
    Method mtdYCSBClientMain;

    public CassandraBench(Config config, File scratch, File data) throws Exception {
        super(config, scratch, data, false);
    }

    private static void addToSystemClassLoader(List<URL> urls) throws Exception {
        if (!(ClassLoader.getSystemClassLoader() instanceof URLClassLoader)) {
            throw new RuntimeException("Currently cassandra benchmark requires Java version <= 1.8, you have " + System.getProperty("java.version"));
        }

        try {
            URLClassLoader sysCL = (URLClassLoader) ClassLoader.getSystemClassLoader();
            Method addURL = URLClassLoader.class.getDeclaredMethod("addURL", new Class[] { URL.class });
            addURL.setAccessible(true);
            for (URL url : urls) {
                addURL.invoke(sysCL, url);
            }
        } catch (NoSuchMethodException | IllegalAccessException e) {
            e.printStackTrace();
            System.err.println("This version of Java (" + System.getProperty("java.version") + 
                    ") does not support the SystemClassLoader hack.");
            throw e;
        }
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
            // XXX: requires Java <= 1.8 to work; for later versions, use reflection API.
            addToSystemClassLoader(findJars(dirScratchJar, Arrays.asList(CASSANDRA_JARS)));

            System.setProperty("java.library.path", dirLibSigar.getPath());
            System.setProperty("cassandra.storagedir", dirCassandraStorage.toString());
            System.setProperty("cassandra.logdir", dirCassandraLog.toString());
            System.setProperty("cassandra.config", ymlConf.toURI().toString());
            System.setProperty("cassandra.logback.configurationFile", xmlLogback.toString());
            System.setProperty("cassandra-foreground", "yes");
            cassandra = new EmbeddedCassandraService();
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
        cassandra.start();

        clYCSB = new URLClassLoader(findJars(dirScratchJar, Arrays.asList(YCSB_JARS))
                                        .toArray(new URL[0]), clOriginal);
        clsYCSBClient = clYCSB.loadClass("com.yahoo.ycsb.Client");
        mtdYCSBClientMain = clsYCSBClient.getMethod("main", String[].class);
        prepareYCSBArgs(size);
        prepareYCSBCQL();
    }

    private void prepareYCSBCQL() {
        Cluster cluster = Cluster.builder()
                .addContactPoint("localhost")
                .withPort(DatabaseDescriptor.getNativeTransportPort())
                .build();
        Session session = cluster.connect();
        session.execute("CREATE KEYSPACE ycsb WITH REPLICATION = {'class': 'SimpleStrategy', 'replication_factor': 1};");
        session.execute("USE ycsb;");
        session.execute("CREATE TABLE usertable (" +
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
        session.close();
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
        Thread.currentThread().setContextClassLoader(clYCSB);

        // load workload
        ycsbWorkloadArgs[ycsbWorkloadArgs.length - 1] = "-load";
        mtdYCSBClientMain.invoke(null, (Object)ycsbWorkloadArgs);
        
        // run transactions
        ycsbWorkloadArgs[ycsbWorkloadArgs.length - 1] = "-t";
        mtdYCSBClientMain.invoke(null, (Object)ycsbWorkloadArgs);
        System.out.println("DaCapo: finished iteration");
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
