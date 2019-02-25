/*
 * Copyright (c) 2018 The Australian National University.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License v2.0.
 * You may obtain the license at
 *
 *    http://www.opensource.org/licenses/apache2.0.php
 */

package org.dacapo.harness;

import org.dacapo.parser.Config;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

/**
 * Dacapo benchmark harness for H2O
 *
 * date:  $Date: 2018-2-10 $
 */

public class H2O extends Benchmark{

    private String [] H2O_JARS= {
            "commons-beanutils-1.9.3.jar",
            "commons-collections-3.2.2.jar",
            "commons-lang-2.6.jar",
            "commons-logging.jar",
            "ezmorph-1.0.6.jar",
            "json-lib-2.4-jdk15.jar"
    };

    private String[] args;

    public H2O(Config config, File scratch, File data) throws Exception {
        super(config, scratch, data);
        Class<?> clazz = Class.forName("water.H2OApp", true, loader);
        this.method = clazz.getMethod("main", String[].class);
    }

    @Override
    protected void prepare(String size) throws Exception {
        super.prepare(size);
        args = config.preprocessArgs(size, scratch, data);
        setupH2O();
    }

    private void setupH2O() {
        File dirScratchJar = new File(scratch, "jar");
        addToSystemClassLoader(findJars(dirScratchJar, H2O_JARS));
    }

    private void addToSystemClassLoader(List<URL> urls){
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
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            System.err.println("This version of Java (" + System.getProperty("java.version") +
                    ") does not support the SystemClassLoader hack.");
        }
    }

    private List<URL> findJars(File dir, String[] jarNames) {
        List<URL> jars = new ArrayList<>();
        for (String jarName : jarNames) {
            File jar = new File(dir, jarName);
            try {
                URL url = jar.toURI().toURL();
                jars.add(url);
            } catch (MalformedURLException e) {
                System.err.println("Unable to create URL for jar: " + jarName);
                e.printStackTrace();
                System.exit(-1);
            }
        }
        return jars;
    }

    @Override
    public void iterate(String size) throws Exception {
        this.method.invoke(null,  (Object) args);
    }
}
