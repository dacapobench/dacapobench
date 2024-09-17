/*
 * Copyright (c) 2009 The Australian National University.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License v2.0.
 * You may obtain the license at
 * 
 *    http://www.opensource.org/licenses/apache2.0.php
 */
package org.dacapo.daytrader;

import java.net.HttpURLConnection;
import java.net.URL;

/**
 * date:  $Date: 2009-12-24 11:19:36 +1100 (Thu, 24 Dec 2009) $
 * id: $Id: DaCapoClientRunner.java 738 2009-12-24 00:19:36Z steveb-oss $
 */
public class DaCapoClientRunner {

  public static void initialize(int logNumSessions, int numThreads, boolean useBeans) {
    try {
      URL url = new URL("http://localhost:"+Launcher.DAYTRADER_PORT+"/daytrader/config?action=dacapoBuild&size=" + logNumSessions + (useBeans ? "&bean=-b" : "" ));

      HttpURLConnection connection = (HttpURLConnection) url.openConnection();

      // Request for creating tables
      connection.setRequestMethod("GET");

      if (connection.getResponseCode() != 200) {
        throw new RuntimeException("Failed for creating tables!");
      }

    } catch (Exception e) {
      System.err.print("Exception initializing client: " + e.toString());
      e.printStackTrace();
      System.exit(-1);
    }
  }

  public static void prepare(int logNumSessions, int numThreads, boolean useBeans) {
    try {
      URL url = new URL("http://localhost:"+Launcher.DAYTRADER_PORT+"/daytrader/config?action=dacapoPrepare&size=" + logNumSessions + (useBeans ? "&bean=-b" : "" ));

      HttpURLConnection connection = (HttpURLConnection) url.openConnection();

      // Request for pre-iteration prepare
      connection.setRequestMethod("GET");

      if (connection.getResponseCode() != 200) {
        throw new RuntimeException("Failed for preparing transactionses!");
      }

    } catch (Exception e) {
      System.err.print("Exception preparing iteration: " + e.toString());
      e.printStackTrace();
      System.exit(-1);
    }
  }

  public static void runIteration(int logNumSessions, int numThreads, int timeoutms, boolean useBeans) {

    try {
      URL url = new URL("http://localhost:"+Launcher.DAYTRADER_PORT+"/daytrader/config?action=dacapoRun&size=" + logNumSessions + (useBeans ? "&bean=-b" : "" ));

      HttpURLConnection connection = (HttpURLConnection) url.openConnection();

      // Request for running transactions
      connection.setRequestMethod("GET");

      if (connection.getResponseCode() != 200){
        throw new RuntimeException("Failed for running transactions!");
      }
    } catch (Exception e) {
      System.err.print("Exception running client iteration: " + e.toString());
      e.printStackTrace();
      System.exit(-1);
    }
  }
}
