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
import java.net.ConnectException;
import java.net.URL;

import org.jboss.modules.Main;

/**
 * Dacapo benchmark harness for tradesoap.
 */
public class DaCapoServerRunner {

  private static final int PAUSE_MS = 200;

  /**
   * Start the server to deploy DayTrader ejb application, and wait until it is available
   */
  public static void initialize() {
    try {
      /* Asynchronously start server */
      Main.main(new String[] {"org.jboss.as.standalone", "-c", "standalone-full.xml"});

      /* Now wait until either the server is up */
      URL url = new URL("http://localhost:"+System.getProperty("jboss.http.port")+"/daytrader");
      while (true) {
        try {
          HttpURLConnection con = (HttpURLConnection) url.openConnection();
          if (con.getResponseCode() == HttpURLConnection.HTTP_OK) {
            System.err.println("Success. Daytrader returned: " + con.getResponseMessage());
            return;  // we're up, so can return
          } else {
            System.err.println("Got " + con.getResponseCode());
          }
        } catch (ConnectException e) {
          Thread.sleep(PAUSE_MS); // connection refused, try again shortly
        } catch (IOException e) {
          System.err.println("Exception while waiting for server: " + e.toString());
        }
      }
    } catch (Throwable e) {
      System.err.println("Exception initializing server: " + e.toString());
    }

    // Failed to start
    System.err.println("VM was forced to quit without executing the shutdown hook");
    System.exit(-1);
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
