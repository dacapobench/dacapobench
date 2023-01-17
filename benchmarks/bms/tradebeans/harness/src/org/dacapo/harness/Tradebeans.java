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
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;

import org.dacapo.harness.Benchmark;
import org.dacapo.parser.Config;

/**
 * Dacapo benchmark harness for tradebeans.
 * 
 * date:  $Date: 2009-12-24 11:19:36 +1100 (Thu, 24 Dec 2009) $
 * id: $Id: Tradebeans.java 738 2009-12-24 00:19:36Z steveb-oss $
 */

public class Tradebeans extends Benchmark {

  private Method initializeMethod;
  private Method shutdownMethod;

  public Tradebeans(Config config, File scratch, File data) throws Exception {
    super(config, scratch, data, false);

    // Find the launcher
    Class<?> clazz = Class.forName("org.dacapo.daytrader.Launcher", true, loader);
    this.initializeMethod = clazz.getMethod("initialize", new Class[] { File.class, File.class, Integer.TYPE, Integer.TYPE, Integer.TYPE, Boolean.TYPE});
    this.method = clazz.getMethod("performIteration", new Class[] {});
    this.shutdownMethod = clazz.getMethod("shutdown", new Class[] {});
  }

  @Override
  protected void prepare(String size) throws Exception {
    String[] args = config.preprocessArgs(size, scratch, data);
    int logNumSessions = 3;
    int timeoutms = 0;
    final int threads = config.getThreadCount(size);
    if (args.length == 2) {
      logNumSessions = Integer.parseInt(args[0]);
      timeoutms = 1000*Integer.parseInt(args[1])*iterations;
      timeoutms = (int) (timeoutms*Float.parseFloat(timeoutDialation));
    } else {
      System.err.println("Quitting.   Bad arguments: "+args);
      System.exit(1);
    }
    System.out.println("Launching the server with timeout of "+(timeoutms/1000)+" seconds (use -f to adjust timeout dialation).");
    PrintStream stdout = System.out;
    // Hide server starting messages
    emptyOutput();

    initializeMethod.invoke(null, data, scratch, threads, logNumSessions, timeoutms, true);

    // stdout for iterate
    System.setOut(stdout);
  }

  public void cleanup() {
    try {
      shutdownMethod.invoke(null);
    }catch (Exception e) {
      e.printStackTrace();
    }
    System.out.println("Shutting down Wildfly...");
    if (!getPreserve()) {
      deleteTree(new File(scratch, "wildfly-26.1.2.Final"));
      deleteTree(new File(scratch, "jar"));
    }
  }

  public void iterate(String size) throws Exception {
    if (getVerbose())
      System.out.println("tradebeans benchmark starting");
    method.invoke(null);
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
