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

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.io.IOException;

import org.dacapo.parser.Config;

/**
 */
public class Spring extends Benchmark {

    // private Constructor lc;
    // private Method launching;
    // private Method performIteration;
    // private Object launcherInstance;
    // private Method shutdown;
    private String[] args;

    public Spring(Config config, File scratch, File data) throws Exception {
        super(config, scratch, data, false, true);
        // Class launcher = Class.forName("org.dacapo.kafka.Launcher", true, this.loader);
        // lc = launcher.getConstructor(File.class, File.class, String[].class);
        // launching = launcher.getMethod("launching");
        // performIteration = launcher.getMethod("performIteration");
        // shutdown = launcher.getMethod("shutdown");
    }

    @Override
    protected void prepare(String size) throws Exception {
        super.prepare(size);
        args = config.preprocessArgs(size, scratch, data);
        // File kafkaData = new File(data, "dat"+File.separator+"kafka");
        // launcherInstance = lc.newInstance(this.scratch, kafkaData, args);
        // Thread.currentThread().setContextClassLoader(loader);
        // launching.invoke(launcherInstance);


        // https://www.toptal.com/spring-boot/spring-boot-application-programmatic-launch

        String pathToJar = data+File.separator+"jar"+File.separator+"spring"+File.separator+"spring-petclinic-2.4.5.jar";

        loadJar(pathToJar);

    }

    @Override
    public void iterate(String size) throws Exception {
        LatencyReporter.initialize(Integer.parseInt(args[1]), 1);
        // System.setProperty("TaskState", "Waiting");
        // performIteration.invoke(launcherInstance);
    }

    @Override
    public void postIteration(String size) throws Exception {
        // shutdown.invoke(launcherInstance);
        super.postIteration(size);
    }

    public static void loadJar(final String pathToJar) throws Exception {
		// Class name to Class object mapping.
		final Map<String, Class<?>> classMap = new HashMap<>();

		final JarFile jarFile = new JarFile(pathToJar);
		final Enumeration<JarEntry> jarEntryEnum = jarFile.entries();

		final URL[] urls = { new URL("jar:file:" + pathToJar + "!/") };
		final URLClassLoader urlClassLoader = URLClassLoader.newInstance(urls);

        while (jarEntryEnum.hasMoreElements()) {

            final JarEntry jarEntry = jarEntryEnum.nextElement();
            final String jarEntryName = jarEntry.getName();
            
            if (jarEntryName.startsWith("org/springframework/boot")
            && jarEntryName.endsWith(".class") == true) {
            
                int endIndex = jarEntryName.lastIndexOf(".class");
                
                String className = jarEntryName.substring(0, endIndex).replace('/', '.');
                
                try {
                    final Class<?> loadedClass = urlClassLoader.loadClass(className);
                    classMap.put(loadedClass.getName(), loadedClass);
                }
                catch (final ClassNotFoundException ex) {
                
                }
            }
        }
        jarFile.close();


        // Create JarFileArchive(File) object, needed for JarLauncher.
        final Class<?> jarFileArchiveClass = classMap.get("org.springframework.boot.loader.archive.JarFileArchive");
        final Constructor<?> jarFileArchiveConstructor = jarFileArchiveClass.getConstructor(File.class);
        final Object jarFileArchive = jarFileArchiveConstructor.newInstance(new File(pathToJar));

        final Class<?> archiveClass = classMap.get("org.springframework.boot.loader.archive.Archive");
				
        final Class mainClass = classMap.get("org.springframework.boot.loader.JarLauncher");

        // Create JarLauncher object using JarLauncher(Archive) constructor. 
        final Constructor<?> jarLauncherConstructor = mainClass.getDeclaredConstructor(archiveClass);

        jarLauncherConstructor.setAccessible(true);
        final Object jarLauncher = jarLauncherConstructor.newInstance(jarFileArchive);

        // Invoke JarLauncher#launch(String[]) method.
        final Class<?> launcherClass = 	classMap.get("org.springframework.boot.loader.Launcher");

        final Method launchMethod = launcherClass.getDeclaredMethod("launch", String[].class);
        launchMethod.setAccessible(true);
		launchMethod.invoke(jarLauncher, new Object[]{new String[0]});

    }
}
