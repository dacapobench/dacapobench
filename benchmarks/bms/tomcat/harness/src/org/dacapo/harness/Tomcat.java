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
import java.lang.reflect.Constructor;

import org.dacapo.harness.Benchmark;
import org.dacapo.parser.Config;

/**
 * Benchmark harness for the Tomcat benchmark
 * 
 * @date $Date: 2009-12-24 11:19:36 +1100 (Thu, 24 Dec 2009) $
 * @id $Id: Tomcat.java 738 2009-12-24 00:19:36Z steveb-oss $
 * 
 */
public class Tomcat extends Benchmark {

  private static final int PORT = 7080;// Integer.valueOf(System.getProperty("dacapo.tomcat.port", "7080"));
  private final Class<?> clazz;
  private final Constructor<Runnable> clientConstructor;
  private final Object controller;

  /**
   * Benchmark constructor - invoked reflectively by the harness.
   * 
   * @param config Benchmark configuration object
   * @param scratch Scratch directory
   * @throws Exception When something goes wrong
   */
  public Tomcat(Config config, File scratch) throws Exception {
    super(config, scratch, false);
    this.clazz = Class.forName("org.dacapo.tomcat.Control", true, loader);
    this.method = clazz.getMethod("exec", String.class);

    /* Create a constructor for the tomcat controller */
    Constructor<?> controlConstructor = clazz.getConstructor(File.class, ClassLoader.class, int.class);
    this.controller = controlConstructor.newInstance(scratch, loader, PORT);

    /* Create a constructor for the tomcat client */
    @SuppressWarnings("unchecked")
    Class<Runnable> clientClass = (Class<Runnable>) Class.forName("org.dacapo.tomcat.Client", true, loader);
    this.clientConstructor = clientClass.getConstructor(File.class, int.class, int.class, boolean.class, int.class);
  }

  /**
   * One-off setup
   */
  @Override
  public void prepare(String size) throws Exception {
    super.prepare(size);

    try {
      useBenchmarkClassLoader();
      try {
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog");
        System.setProperty("org.apache.commons.logging.simplelog.defaultlog", "info");
        System.setProperty("catalina.home", scratch.getAbsolutePath());
        System.setProperty("catalina.config", new File(fileInScratch("catalina.properties")).toURL().toExternalForm());
        method.invoke(controller, "prepare");

        System.out.println("Server thread created");
      } finally {
        revertClassLoader();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * After each iteration, delete the output files
   */
  @Override
  public void postIteration(String size) throws Exception {
    super.postIteration(size);
  }

  /**
   * An iteration of the benchmark - runs in the benchmark classloader
   */
  @Override
  public void iterate(String size) throws Exception {
    System.out.println("Loading web application");
    method.invoke(controller, "startIteration");

    final int threadCount = config.getThreadCount(size);
    String[] args = config.getArgs(size);
    final int iterations = Integer.parseInt(args[0]);

    /*
     * In case the # iterations doesn't evenly divide among the processors, we
     * ensure that some threads do one more iteration than others
     */
    final int iterationsPerClient = iterations / threadCount;
    final int oddIterations = iterations - (iterationsPerClient * threadCount);

    final Thread[] threads = new Thread[threadCount];
    System.out.println("Creating client threads");
    for (int i = 0; i < threadCount; i++) {
      Runnable client = clientConstructor.newInstance(scratch, i, iterationsPerClient + (i < oddIterations ? 1 : 0), getVerbose(), PORT);
      threads[i] = new Thread(client);
      threads[i].start();
    }
    System.out.println("Waiting for clients to complete");
    for (int i = 0; i < threadCount; i++) {
      threads[i].join();
    }
    System.out.println("Client threads complete ... unloading web application");
    method.invoke(controller, "stopIteration");
  }

  @Override
  protected void postIterationCleanup(String size) {
    super.postIterationCleanup(size);
    /* Delete the tomcat cache */
    deleteTree(new File(scratch, "work"));
  }

  /**
   * @see org.dacapo.harness.Benchmark#cleanup()
   */
  @Override
  public void cleanup() {
    try {
      method.invoke(controller, "cleanup");
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    System.out.println("Server stopped ... iteration complete");
    super.cleanup();
  }

  @SuppressWarnings("unused")
  private void dumpThreads() {
    ThreadGroup tg = Thread.currentThread().getThreadGroup();
    int nThreads = tg.activeCount();
    Thread[] threads = new Thread[nThreads * 2];
    nThreads = Thread.enumerate(threads);
    System.out.printf("==================== Dumping %d Threads: ====================%n", nThreads);
    System.out.flush();
    for (int i = 0; i < nThreads; i++) {
      if (threads[i] != null) {
        System.out.print(threads[i].getName() + ": ");
        StackTraceElement[] stack = threads[i].getStackTrace();
        for (int j = 0; j < stack.length; j++) {
          for (int k = 0; k < j; k++)
            System.out.print("  ");
          System.out.println(stack[j].getClassName() + "." + stack[j].getMethodName() + ":" + stack[j].getLineNumber() + " <- ");
        }
      } else {
        System.out.print("null ");
      }
      System.out.flush();
    }
    System.out.println();
    System.out.flush();
    System.out.printf("==================== Thread Dump End ====================%n");
  }
}
