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
    private String ip = "127.0.0.1";
    private String port = "54321";

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

        // Launch the h2o server
        useBenchmarkClassLoader();
        // use these for debugging: "-log_level", "DEBUG", "-log_dir", scratch+File.separator+"h2o.log"
        this.method.invoke(null,  (Object) new String[] {"-ip", ip, "-port", port});
    }

    @Override
    public void iterate(String size) throws Exception {

        PrintStream savedOut = System.out;
        PrintStream savedErr= System.err;
        // Store the standard output
        emptyOutput();

        ClientRunner.running("http://" + ip + ":" + port, args[0], args[1], args[2], args[3], savedOut, savedErr);

        System.setOut(savedOut);
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
