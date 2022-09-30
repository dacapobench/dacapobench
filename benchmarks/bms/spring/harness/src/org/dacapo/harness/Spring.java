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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.FileReader;

/**
 */
public class Spring extends Benchmark {
    private Method launch;
    private final Constructor<Runnable> clientConstructor;
    private final Method reset;

    private String[] args;
    private String[] requests;


    public Spring(Config config, File scratch, File data) throws Exception {
        super(config, scratch, data, false, false);

        Class launcher = Class.forName("org.dacapo.spring.Launcher", true, this.loader);
        launch = launcher.getMethod("launch", String.class);
        Class client = Class.forName("org.dacapo.spring.Client", true, this.loader);
        this.clientConstructor = client.getConstructor(int.class,  org.dacapo.harness.LatencyReporter.class);
        this.reset = client.getMethod("reset", String[].class, int.class, int.class);
    }
    
    @Override
    protected void prepare(String size) throws Exception {
        super.prepare(size);
        args = config.preprocessArgs(size, scratch, data);
        loadRequests(Integer.parseInt(args[0]));

        // https://www.toptal.com/spring-boot/spring-boot-application-programmatic-launch

        String pathToJar = data+File.separator+"jar"+File.separator+"spring"+File.separator+"spring-petclinic-2.7.3.jar";

        Thread.currentThread().setContextClassLoader(loader);
        launch.invoke(null, pathToJar);
    }


    private void loadRequests(int sessions) {
        requests = new String[sessions];

        String filename = data+File.separator+"dat"+File.separator+"spring"+File.separator+"requests.txt";

        try {
            BufferedReader in = new BufferedReader(new FileReader(filename));
            try {
                for (int i = 0; i < requests.length; i++) 
                    requests[i] = in.readLine();
            } catch (java.io.IOException e) {
                System.err.println("Exception while reading from "+filename+":"+e);
            }
        } catch (java.io.FileNotFoundException e) {
            System.err.println("Failed to open "+filename+":"+e);
        }
        System.err.println("Loaded "+requests.length+" requests");
    }

    @Override
    public void iterate(String size) throws Exception { 
        int threadCount = config.getThreadCount(size);
        final Thread[] threads = new Thread[threadCount];
        int stride = Integer.parseInt(args[1]);
        LatencyReporter.initialize(requests.length, threadCount, stride);
        LatencyReporter.requestsStarting();
        for (int i = 0; i < threadCount; i++) {
            LatencyReporter lr = new LatencyReporter(i, threadCount, requests.length, stride);
            Runnable client = clientConstructor.newInstance(i, lr);
            threads[i] = new Thread(client);
            threads[i].start();
        }

        // Wait for clients to complete
        for (int i = 0; i < threadCount; i++) {
            threads[i].join();
        }
        System.out.println();
        LatencyReporter.requestsFinished();
    }
    
    @Override
    public void preIteration(String size) throws Exception {
        reset.invoke(null, requests, Integer.parseInt(args[1]), config.getThreadCount(size));
        super.preIteration(size);
    }
}
