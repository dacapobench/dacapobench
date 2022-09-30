/*
 * Copyright (c) 2009 The Australian National University.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License v2.0.
 * You may obtain the license at
 * 
 *    http://www.opensource.org/licenses/apache2.0.php
 */
package org.dacapo.daytrader;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import org.jboss.modules.Main;

/**
 * Dacapo benchmark harness for tradesoap.
 * 
 * date:  $Date: 2009-12-24 11:19:36 +1100 (Thu, 24 Dec 2009) $
 * id: $Id: DaCapoServerRunner.java 738 2009-12-24 00:19:36Z steveb-oss $
 */
public class DaCapoServerRunner {

  /**
   * Start the server and deploy DayTrader ejb application
   */
  public static void initialize() {
    try {

      Main.main(new String[] {"org.jboss.as.standalone", "-c", "standalone-full.xml"});

      // Checking if server started
      URL url = new URL("http://localhost:8080/daytrader");

      while (true) {
        try {
          Thread.sleep(5000);
          HttpURLConnection connection = (HttpURLConnection) url.openConnection();
          if (connection.getResponseCode() != 200) throw new RuntimeException();
        } catch (InterruptedException|IOException e) {
          System.err.print("Exception initializing server: " + e.toString());
          System.err.print("VM was forced to quit without executing the shutdown hook");
          Runtime.getRuntime().halt(1);
        }
        break;
      }

    } catch (Throwable e) {
      System.err.print("Exception initializing server: " + e.toString());
      System.err.print("VM was forced to quit without executing the shutdown hook");
      Runtime.getRuntime().halt(1);
    }
  }

  private static void makeExecutable(String scriptPath){
    try {
      // Give the script
      ProcessBuilder builder = new ProcessBuilder("/bin/chmod", "755",scriptPath);
      builder.start().waitFor();
    } catch (InterruptedException | IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Shutdown the server
   */
  public static void shutdown(){
    try {
      String scriptPath;

      if (System.getProperty("os.name").toLowerCase().contains("win"))
        scriptPath = System.getProperty("jboss.home.dir") + File.separator + "bin" + File.separator + "jboss-cli.bat";
      else{
        scriptPath = System.getProperty("jboss.home.dir") + File.separator + "bin" + File.separator + "jboss-cli.sh";

        // Make the script executable
        makeExecutable(scriptPath);
      }

      // Start the wildfly servers
      new ProcessBuilder(scriptPath,"--connect","command=:shutdown").start().waitFor();

    } catch (Exception e) {
      System.err.print("Exception initializing server: " + e.toString());
      e.printStackTrace();
      System.exit(-1);
    }
  }
}
