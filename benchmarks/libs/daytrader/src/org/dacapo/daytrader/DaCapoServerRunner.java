/*
 * Copyright (c) 2009 The Australian National University.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License v2.0.
 * You may obtain the license at
 * 
 *    http://www.opensource.org/licenses/apache2.0.php
 */
package org.dacapo.daytrader;

import java.io.InputStream;

import org.apache.geronimo.cli.daemon.DaemonCLParser;
import org.apache.geronimo.kernel.Kernel;
import org.apache.geronimo.kernel.KernelFactory;
import org.apache.geronimo.kernel.config.ConfigurationUtil;
import org.apache.geronimo.kernel.log.GeronimoLogging;
import org.apache.geronimo.kernel.util.Main;

/**
 * Dacapo benchmark harness for tradesoap.
 * 
 * @date $Date: 2009-12-24 11:19:36 +1100 (Thu, 24 Dec 2009) $
 * @id $Id: DaCapoServerRunner.java 738 2009-12-24 00:19:36Z steveb-oss $
 */
public class DaCapoServerRunner {
  private static Kernel kernel = null;
  private static Thread serverThread = null;

  public static void initialize() {
    try {
      GeronimoLogging.initialize(GeronimoLogging.ERROR);
      final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
      kernel = KernelFactory.newInstance().createKernel("DaCapoServer");
      kernel.boot();
      InputStream in = classLoader.getResourceAsStream("META-INF/config.ser");

      ConfigurationUtil.loadBootstrapConfiguration(kernel, in, classLoader, true);
      final DaemonCLParser parser = new DaemonCLParser(System.out);
      final Main main = (Main) kernel.getGBean(Main.class);
      parser.parse(new String[] { "--quiet" });
      serverThread = new Thread(new Runnable() {
        public void run() {
          Thread.currentThread().setContextClassLoader(main.getClass().getClassLoader());
          main.execute(parser);
        }
      });
      serverThread.start();
    } catch (Exception e) {
      System.err.print("Exception initializing server: " + e.toString());
      e.printStackTrace();
      System.exit(-1);
    }
  }

  public static void shutdown() {
    while (serverThread.isAlive()) {
      serverThread.interrupt();
      try {
        serverThread.join();
      } catch (InterruptedException e) {
      }
    }
    serverThread = null;
    kernel.shutdown();
    kernel = null;
  }
}
