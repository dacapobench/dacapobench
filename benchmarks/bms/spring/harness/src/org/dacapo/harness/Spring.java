/*
 * Copyright (c) 2021 The Australian National University.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License v2.0.
 * You may obtain the license at
 *
 *    http://www.opensource.org/licenses/apache2.0.php
 */
package org.dacapo.harness;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;

import org.dacapo.parser.Config;
import org.dacapo.harness.LatencyReporter;
// import org.dacapo.spring.Client;
// import org.dacapo.spring.Launcher;

/**
 */
public class Spring extends Benchmark {

    // private Constructor lc;
    // private Method launch;
    // private Method performIteration;
    // private Object launcherInstance;
    // private Method shutdown;


    private Method launch;
 //   private Method iterate;
    private final Constructor<Runnable> clientConstructor;

    private String[] args;

    public Spring(Config config, File scratch, File data) throws Exception {
        super(config, scratch, data, false, true);

//        useBenchmarkClassLoader();
// System.err.println("AAAA");
//         Class<?> clazza = Class.forName("org.apache.commons.httpclient.HttpClient", true, loader);
//         System.err.println("BBBB");

//         Class<?> clazzb = Class.forName("org.apache.commons.httpclient.HttpConnectionManager", true, loader);
//         System.err.println("CCCC");




        Class launcher = Class.forName("org.dacapo.spring.Launcher", true, this.loader);
        launch = launcher.getMethod("launch", String.class);
        Class client = Class.forName("org.dacapo.spring.Client", true, this.loader);

        this.clientConstructor = client.getConstructor(int.class, org.dacapo.harness.LatencyReporter.class);

     //   iterate = client.getMethod("get");

    }

    @Override
    protected void prepare(String size) throws Exception {
        super.prepare(size);
        args = config.preprocessArgs(size, scratch, data);


        // https://www.toptal.com/spring-boot/spring-boot-application-programmatic-launch

        String pathToJar = data+File.separator+"jar"+File.separator+"spring"+File.separator+"spring-petclinic-2.4.5.jar";

        Thread.currentThread().setContextClassLoader(loader);
        launch.invoke(null, pathToJar);
      //  Launcher.launch(pathToJar);
    }

    @Override
    public void iterate(String size) throws Exception {
        LatencyReporter.initialize(Integer.parseInt(args[1]), 1);
        // System.setProperty("TaskState", "Waiting");
        // performIteration.invoke(launcherInstance);
        //Client.get();

        int threadCount = config.getThreadCount(size);

        final Thread[] threads = new Thread[threadCount];
        LatencyReporter.initialize(8*threadCount, threadCount);
        for (int i = 0; i < threadCount; i++) {
            LatencyReporter lr = new LatencyReporter(i, threadCount, 8*threadCount);
            Runnable client = clientConstructor.newInstance(i, lr);
            threads[i] = new Thread(client);
            threads[i].start();
        }
        System.out.println("Waiting for clients to complete");
        for (int i = 0; i < threadCount; i++) {
            threads[i].join();
        }
    }

    @Override
    public void postIteration(String size) throws Exception {
        // shutdown.invoke(launcherInstance);
        super.postIteration(size);
    }


}
