/*
 * Copyright (c) 2009-2020 The Australian National University.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License v2.0.
 * You may obtain the license at
 * 
 *    http://www.opensource.org/licenses/apache2.0.php
 */
package org.dacapo.harness;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.lang.reflect.Constructor;

import sun.misc.Unsafe;
import java.lang.reflect.Field;
import java.util.List;

import org.dacapo.harness.Benchmark;
import org.dacapo.parser.Config;

/**
 * Benchmark harness for the Tomcat benchmark
 * 
 * date:  $Date: 2009-12-24 11:19:36 +1100 (Thu, 24 Dec 2009) $
 * id: $Id: Tomcat.java 738 2009-12-24 00:19:36Z steveb-oss $
 * 
 */
public class Tomcat extends Benchmark {

  private static final int PORT = 8080;// Integer.valueOf(System.getProperty("dacapo.tomcat.port", "7080"));
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
  public Tomcat(Config config, File scratch, File data) throws Exception {
    super(config, scratch, data, false, false);
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

    prepareScratch();

    try {
      useBenchmarkClassLoader();

      /*
       * FIXME
       * 
       * This workaround silences JDK11 warnings relating to use of
       * reflection.
       *
       * Specifically, catalina generates the following warning:
       * 
       *  WARNING: Illegal reflective access by org.apache.catalina.loader.WebappClassLoaderBase (file:[...]/tomcat/catalina.jar) to field java.io.ObjectStreamClass$Caches.localDescs
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
      try {
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog");
        System.setProperty("org.apache.commons.logging.simplelog.defaultlog", "info");
        System.setProperty("catalina.home", data.getAbsolutePath()+File.separator+"dat"+File.separator+"tomcat");
        System.setProperty("catalina.base", scratch.getAbsolutePath()+File.separator+"tomcat");
        System.setProperty("catalina.config", new File(fileInData("dat"+File.separator+"tomcat"+File.separator+"catalina.properties")).toURL().toExternalForm());
        String jar = fileInData("jar"+File.separator+"tomcat"+File.separator+"*.jar");
        System.setProperty("catalina.cl.repo", jar);

        method.invoke(controller, "prepare");

        System.out.println("Server thread created");
      } finally {
        revertClassLoader();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void prepareScratch() {
    mkdir("work");
    mkdir("temp");
    mkdir("logs");
    cp("conf");
    cp("lib");
    cp("webapps"+File.separator+"manager");
    cp("webapps"+File.separator+"examples"+File.separator+"jsp");
    cp("webapps"+File.separator+"examples"+File.separator+"WEB-INF");
  }

  private void mkdir(String path) {
    File dir = new File(scratch.getAbsolutePath()+File.separator+"tomcat"+File.separator+path);
    dir.mkdirs();
  }

  private void cp(String path) {
    try {

    Path orig = Paths.get(data.getAbsolutePath()+File.separator+"dat"+File.separator+"tomcat"+File.separator+path);
    Path tmp = Paths.get(scratch.getAbsolutePath()+File.separator+"tomcat"+File.separator+path);
    Files.walkFileTree(orig, new Copy(orig, tmp));
    } catch (Exception e) {
      System.err.println("Failed to copy path "+path);
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

    Field f = Class.forName("org.dacapo.tomcat.Client", true, loader).getField("BATCH_SIZE");
    final int BATCH_SIZE = (int) f.get(null);
    int requests = iterations * BATCH_SIZE;

    final Thread[] threads = new Thread[threadCount];
    LatencyReporter.initialize(requests, threadCount, BATCH_SIZE);
    LatencyReporter.requestsStarting();
    for (int i = 0; i < threadCount; i++) {
      Runnable client = clientConstructor.newInstance(scratch, i, iterations, getVerbose(), PORT);
      threads[i] = new Thread(client);
      threads[i].start();
    }
    for (int i = 0; i < threadCount; i++) {
      threads[i].join();
    }
    LatencyReporter.requestsFinished();
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

  public class Copy extends SimpleFileVisitor<Path> {
    private Path src;
    private Path tgt;
 
    public Copy(Path src, Path tgt) {
        this.src = src;
        this.tgt = tgt;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attr) {
      try {
        Path tgtFile = tgt.resolve(src.relativize(file));
        Files.copy(file, tgtFile);
      } catch (IOException ex) {
        System.err.println(ex);
      }
      return FileVisitResult.CONTINUE;
    }
 
    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attr) {
      try {
        Path newDir = tgt.resolve(src.relativize(dir));
        Files.createDirectories(newDir);
      } catch (IOException ex) {
        System.err.println(ex);
      }
      return FileVisitResult.CONTINUE;
    }
  }
}
