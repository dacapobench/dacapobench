/*
 * Copyright (c) 2006, 2009 The Australian National University.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License v2.0.
 * You may obtain the license at
 * 
 *    http://www.opensource.org/licenses/apache2.0.php
 */
package org.dacapo.harness;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandlerFactory;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.dacapo.parser.Config;

/**
 * Custom class loader for the dacapo benchmarks. Instances of this classloader
 * are created by passing a list of jar files. This allows us to package a
 * benchmark as a set of jar files, rather than having to mix the classes for
 * all the benchmarks into the dacapo jar file.
 * 
 * date:  $Date: 2009-12-24 11:19:36 +1100 (Thu, 24 Dec 2009) $
 * id: $Id: DacapoClassLoader.java 738 2009-12-24 00:19:36Z steveb-oss $
 */
public class DacapoClassLoader extends URLClassLoader {

  private static void sortURLs(URL[] urls) {
    Map<String, URL> m = new TreeMap<>();
    for (URL url : urls) {
      m.put(url.toString(), url);
    }
    int i = 0;
    for (String u : m.keySet()) {
      urls[i++] = m.get(u);
    }
  }
  /**
   * Factory method to create the class loader to be used for each invocation of
   * this benchmark
   * 
   * @param config The config file, which contains information about the jars
   * this benchmark depends on
   * @param scratch The scratch directory (in which some of the jars may be located)
   * @param data The data directory (in which externally packaged jars will be located)
   * @return The class loader in which this benchmark's iterations should
   * execute.
   */
  public static DacapoClassLoader create(Config config, File scratch, File data, Set<URL> jarDeps) {
    DacapoClassLoader rtn = null;
    try {
      URL[] urls = new URL[jarDeps.size()];
      jarDeps.toArray(urls);
      sortURLs(urls);
      if (Benchmark.getVerbose()) {
         System.out.println("Benchmark classpath:");
         for (URL url : urls) {
           System.out.println("  " + url.toString());
         }
      }
      rtn = new DacapoClassLoader(urls, ClassLoader.getSystemClassLoader());
    } catch (Exception e) {
      System.err.println("Unable to create loader for " + config.name + ":");
      e.printStackTrace();
      System.exit(-1);
    }
    return rtn;
  }

  /**
   * @param urls
   */
  public DacapoClassLoader(URL[] urls) {
    super(urls);
  }

  /**
   * @param urls
   * @param parent
   */
  public DacapoClassLoader(URL[] urls, ClassLoader parent) {
    super(urls, parent);
  }

  /**
   * @param urls
   * @param parent
   * @param factory
   */
  public DacapoClassLoader(URL[] urls, ClassLoader parent, URLStreamHandlerFactory factory) {
    super(urls, parent, factory);
  }

  /**
   * Get a list of jars (if any) which should be in the classpath for this
   * benchmark
   * 
   * @param config The config file for this benchmark, which lists the jars
   * @param scratch The scratch directory (in which some of the jars may be located)
   * @param data The data directory (in which externally packaged jars will be located)
   * @return An array of URLs, one URL for each jar
   * @throws MalformedURLException
   */
  private static URL[] getJars(Config config, File scratch, File data) throws MalformedURLException {
    List<URL> jars = new ArrayList<URL>();
    File jardirScratch = new File(scratch, "jar");
    File jardirData = new File(data, "jar");

    if (config.jars != null) {
      for (int i = 0; i < config.jars.length; i++) {
        // first try data directory, then try scratch if not found.
        File jar = new File(jardirData, config.jars[i]);
        if (!jar.exists()) {
          jar = new File(jardirScratch, config.jars[i]);
          if (!jar.exists())
            Data.failDataNotFound(data);
        }
        jars.add(jar.toURI().toURL());  // first convert to URI to avoid deprecation
      }
    }
    return jars.toArray(new URL[jars.size()]);
  }

  /**
   * Reverse the logic of the default classloader, by trying the top-level
   * classes first. This way, libraries packaged with the benchmarks override
   * those provided by the runtime environment.
   */
  @Override
  protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
    // First, check if the class has already been loaded
    Class<?> c = findLoadedClass(name);
    if (c == null) {
       if (name.startsWith("java.")) {
         // security: core classes should go directly to parent
         c = super.loadClass(name, resolve);
       } else {
        try {
          // Next, try to resolve it from the dacapo JAR files
          c = super.findClass(name);
        } catch (ClassNotFoundException e) {
          // And if all else fails delegate to the parent.
          c = super.loadClass(name, resolve);
        }
      }
    }
    if (resolve) {
      resolveClass(c);
    }
    return c;
  }
}
