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


import java.util.Arrays;
import org.apache.geronimo.main.Bootstrapper;
import org.apache.geronimo.main.Main;
import org.apache.geronimo.cli.daemon.DaemonCLParser;
import org.apache.geronimo.cli.shutdown.ShutdownCLParser;
import org.apache.geronimo.main.ScriptLaunchListener;

/**
 * Dacapo benchmark harness for tradesoap.
 * 
 * @date $Date: 2009-12-24 11:19:36 +1100 (Thu, 24 Dec 2009) $
 * @id $Id: DaCapoServerRunner.java 738 2009-12-24 00:19:36Z steveb-oss $
 */
public class DaCapoServerRunner {
  private static Thread serverThread = null;
  private static Bootstrapper boot = null;

  public static void initialize() {
    try {

      boot = new Bootstrapper();

      final DaemonCLParser parser = new DaemonCLParser(System.out);
      parser.parse(new String[] { "-c", "-q" });

      boot.setWaitForStop(true);
      boot.setStartBundles(Arrays.asList("org.apache.geronimo.framework/j2ee-system//car"));
      boot.setLog4jConfigFile("var/log/server-log4j.properties");
      boot.setCleanStorage(((DaemonCLParser)parser).isCleanCache());
      boot.setLaunchListener(new ScriptLaunchListener("server"));

      boot.init();
      boot.launch();
      Main geronimoMain = boot.getMain();

      ClassLoader newTCCL = geronimoMain.getClass().getClassLoader();
      Thread.currentThread().setContextClassLoader(newTCCL);
      geronimoMain.execute(parser);

    } catch (Exception e) {
      System.err.print("Exception initializing server: " + e.toString());
      e.printStackTrace();
      System.exit(-1);
    }
  }

  public static void shutdown(){
    try {
      boot.shutdown(0L);
    } catch (Exception e) {
      System.err.print("Exception initializing server: " + e.toString());
      e.printStackTrace();
      System.exit(-1);
    }
  }
}
