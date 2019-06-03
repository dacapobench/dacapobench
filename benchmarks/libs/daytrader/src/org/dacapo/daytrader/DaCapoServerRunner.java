/*
 * Copyright (c) 2009 The Australian National University.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License v2.0.
 * You may obtain the license at
 * 
 *    http://www.opensource.org/licenses/apache2.0.php
 */
package org.dacapo.daytrader;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Dacapo benchmark harness for tradesoap.
 * 
 * date:  $Date: 2009-12-24 11:19:36 +1100 (Thu, 24 Dec 2009) $
 * id: $Id: DaCapoServerRunner.java 738 2009-12-24 00:19:36Z steveb-oss $
 */
public class DaCapoServerRunner {
  private static Process process;

  /**
   * Start the server and deploy DayTrader ejb application
   */
  public static void initialize() {
    try {
      // Start the wildfly servers
      ProcessBuilder processBuilder =
              new ProcessBuilder(System.getProperty("java.home") + File.separator + "bin" + File.separator + "java",
                      "--add-modules=java.se", "-jar", System.getProperty("jboss.home.dir") + File.separator + "jboss-modules.jar",
                      "-mp", System.getProperty("module.path"),
                      "org.jboss.as.standalone",
                      "-Djboss.home.dir=" + System.getProperty("jboss.home.dir"));

      // Start the process builder
      process = processBuilder.start();

      URL url = new URL("http://localhost:8080/daytrader");

      Thread mt = new Thread(new Runnable() {
        @Override
        public void run() {
          while (true) {
            try {
              Thread.sleep(100);
              HttpURLConnection connection = (HttpURLConnection) url.openConnection();
              if (connection.getResponseCode() != 200) throw new RuntimeException();
            } catch (IOException e) {
              continue;
            } catch (InterruptedException e) {
              e.printStackTrace();
            }
            break;
          }
        }
      });

      System.out.println("Launching the server");
      mt.start();
      mt.join();

      // Print all outputs at the same time
      printOutputs();

    } catch (Exception e) {
      System.err.print("Exception initializing server: " + e.toString());
      e.printStackTrace();
      System.exit(-1);
    }
  }

  /**
   * Print the output from the process
   */
  private static void printOutputs(){
    Thread serverThread = new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          BufferedReader reader =
                  new BufferedReader(new InputStreamReader(process.getInputStream()));
          String line;
          while ((line = reader.readLine()) != null) {
            if (line.contains("DaCapoMarker")) {
              System.out.println(line.substring(line.indexOf("DaCapoMarker") + "DaCapoMarker".length()));
            }
          }
        } catch (IOException ignored) {
        }
      }
    }
    );
    serverThread.start();
  }

  /**
   * Shutdown the server
   */
  public static void shutdown(){
    try {
      process.destroy();
    } catch (Exception e) {
      System.err.print("Exception initializing server: " + e.toString());
      e.printStackTrace();
      System.exit(-1);
    }
  }
}
