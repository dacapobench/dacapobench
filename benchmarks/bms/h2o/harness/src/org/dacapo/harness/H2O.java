/*
 * Copyright (c) 2018-2020 The Australian National University.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License v2.0.
 * You may obtain the license at
 *
 *    http://www.opensource.org/licenses/apache2.0.php
 */

package org.dacapo.harness;

import org.dacapo.h2o.ClientRunner;
import org.dacapo.parser.Config;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import sun.misc.Unsafe;
import java.lang.reflect.Field;

/**
 * Dacapo benchmark harness for H2O
 *
 * date:  $Date: 2018-2-10 $
 */

public class H2O extends Benchmark{

    private String[] args;
    PrintStream savedOut;
    private static final String H2O_IP = "127.0.0.1";
    private static final String H2O_PORT;
    private static final String H2O_REST_URL;

    static {
        if (System.getProperty("dacapo.h2o.port") != null)
            H2O_PORT = System.getProperty("dacapo.h2o.port");
        else
            H2O_PORT = "54321";
        H2O_REST_URL = "http://" + H2O_IP + ":" + H2O_PORT;
    }

    public H2O(Config config, File scratch, File data) throws Exception {
        super(config, scratch, data, false);
        Class<?> clazz = Class.forName("water.H2OApp", true, loader);
        this.method = clazz.getMethod("main", String[].class);
        warnJavaVersionLE(17, "H2O "+config.getDesc("version")+" is only supported for Java versions <= 17."+System.lineSeparator()+"H2O allows you to override its checks by adding '-Dsys.ai.h2o.debug.allowJavaVersions=<MV>' to your command line, where <MV> is the major version you wish to use.");
    }

    @Override
    protected void prepare(String size) throws Exception {
        args = config.preprocessArgs(size, scratch, data);
        emptyOutput();

        System.setProperty("org.eclipse.jetty.util.log.announce", "false");
        System.setProperty("heartbeat.benchmark.enabled", "false");

        /*
         * FIXME
         * 
         * This workaround silences JDK11 warnings relating to use of
         * reflection.
         *
         * Specifically, h2o generates the following warning:
         * 
         * WARNING: Illegal reflective access by ml.dmlc.xgboost4j.java.NativeLibLoader (file:[...]/h2o/h2o.jar) to field java.lang.ClassLoader.usr_paths
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

        // This sets h2o's target key/value cache size (water.MemoryManager).  We set this to an
        // arbitrary 2MB (emperically we find that the workload is very insensitive to this setting)
        // If the target is set to 0, h2o will fall back to its default behavior.
        if (System.getProperty("dacapo.h2o.target") == null)
            System.setProperty("dacapo.h2o.target", args[0]);

        // Launch the h2o server
        useBenchmarkClassLoader();
        // use these for debugging: "-log_level", "DEBUG", "-log_dir", scratch+File.separator+"h2o.log"
        this.method.invoke(null,  (Object) new String[] {"-ip", H2O_IP, "-port", H2O_PORT, "-log_dir", scratch.getAbsolutePath(), "-log_level", "ERROR"});
    }

    @Override
    public void iterate(String size) throws Exception {

        PrintStream savedOut = System.out;
        PrintStream savedErr = System.err;
        // Store the standard output
        emptyOutput();

        ClientRunner.iterate(H2O_REST_URL, args[1], args[2], args[3], args[4], savedOut, savedErr);

        System.setOut(savedOut);
    }

    @Override
    public void postIteration(String size) throws Exception {
        ClientRunner.postIteration(H2O_REST_URL);
    }

    private void emptyOutput(){
        // Store the standard output
        System.setOut(new PrintStream(new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                // Doing nothing
            }
        }));
    }
}
