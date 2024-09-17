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

import sun.misc.Unsafe;
import java.lang.reflect.Field;

import org.dacapo.harness.Benchmark;
import org.dacapo.parser.Config;

/**
 * Dacapo benchmark harness for tradesoap.
 * 
 * date:  $Date: 2009-12-24 11:19:36 +1100 (Thu, 24 Dec 2009) $
 * id: $Id: Tradesoap.java 738 2009-12-24 00:19:36Z steveb-oss $
 */
public class Tradesoap extends Benchmark {
  private Method initializeMethod;
  private Method preapreMethod;
  private Method iterateMethod;
  private Method shutdownMethod;
  private static final int TIMEOUT_MARGIN = 5; // Increase our watchdog timeout by this factor to account for slow machines

  public Tradesoap(Config config, File scratch, File data) throws Exception {
    super(config, scratch, data, false);
    assertJavaVersionGE(11, "Tradesoap requires at least Java version 11.");
    assertJavaVersionLE(21, "Wildfly 26 is incompatible with Java versions higher than 21.");

    Class<?> clazz = Class.forName("org.dacapo.daytrader.Launcher", true, loader);
    this.initializeMethod = clazz.getMethod("initialize", new Class[] { File.class, File.class, Integer.TYPE, Integer.TYPE, Boolean.TYPE});
    this.preapreMethod = clazz.getMethod("performPrepare", new Class[] {});
    this.iterateMethod = clazz.getMethod("performIteration", new Class[] {});
    this.shutdownMethod = clazz.getMethod("shutdown", new Class[] {});
  }

  @Override
  protected void prepare(String size) throws Exception {
    /*
     * FIXME
     *
     * This workaround silences JDK11+ warnings relating to use of
     * reflection.
     *
     * Specifically, catalina generates the following warning:
     *
     *  WARNING: Illegal reflective access by org.wildfly.extension.elytron.SSLDefinitions (jar:file:[...]/dat/lib/daytrader/wildfly-26.1.3.Final/modules/system/layers/base/org/wildfly/extension/elytron/main/wildfly-elytron-integration-18.1.2.Final.jar!/) to method com.sun.net.ssl.internal.ssl.Provider.isFIPS()
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

    String[] args = config.preprocessArgs(size, scratch, data);
    if (args.length != 2) {
      System.err.println("Quitting. Bad arguments: "+args);
      System.exit(1);
    }
    final int logNumSessions = Integer.parseInt(args[0]);
    final int timeEstimate = TIMEOUT_MARGIN * iterations * Integer.parseInt(args[1]);
    final int threads = config.getThreadCount(size);

    int timeout = (int) (timeEstimate * Float.parseFloat(timeoutDialation));
    WatchDog.set(timeout, "tradesoap", "Adjust timeout with the -f command line option.");

    System.out.println("Starting Wildfly...");
    // Silence server startup messages
    PrintStream stdout = System.out;
    emptyOutput();

    initializeMethod.invoke(null, data, scratch, threads, logNumSessions, false);

    // Restore stdout for iterations
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
      deleteTree(new File(scratch, "wildfly-26.1.3.Final"));
      deleteTree(new File(scratch, "jar"));
    }
  }

  @Override
  public void preIteration(String size) throws Exception {
    super.preIteration(size);
    useBenchmarkClassLoader();
    if (getVerbose())
      System.out.println("tradesoap benchmark preparing");
    preapreMethod.invoke(null);
  }

  public void iterate(String size) throws Exception {
    if (getVerbose())
      System.out.println("tradesoap benchmark starting");
    iterateMethod.invoke(null);
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
