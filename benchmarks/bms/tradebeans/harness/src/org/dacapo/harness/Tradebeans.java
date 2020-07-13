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
    this.initializeMethod = clazz.getMethod("initialize", new Class[] { File.class, Integer.TYPE, String.class, Boolean.TYPE });
    this.method = clazz.getMethod("performIteration", new Class[] {});
    this.shutdownMethod = clazz.getMethod("shutdown", new Class[] {});
  }

  @Override
  protected void prepare(String size) throws Exception {
    String[] args = config.preprocessArgs(size, scratch, data);
    String dtSize = "medium";
    if (args.length == 1)
      dtSize = args[0];

    System.out.println("Launching the server");
    PrintStream stdout = System.out;
    // Hide server starting messages
    emptyOutput();

    initializeMethod.invoke(null, new File(data.getAbsolutePath()+File.separator+"dat"+File.separator+"lib"+File.separator+"daytrader"), config.getThreadCount(size), dtSize, true);

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
      deleteTree(new File(scratch, "wildfly-17.0.0.Final"));
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
