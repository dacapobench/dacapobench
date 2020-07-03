/*
 * Copyright (c) 2009 The Australian National University.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License v2.0.
 * You may obtain the license at
 * 
 *    http://www.opensource.org/licenses/apache2.0.php
 */
package org.dacapo.h2;

import java.io.FileWriter;

/**
 * date:  $Date: 2009-12-24 11:19:36 +1100 (Thu, 24 Dec 2009) $
 * id: $Id: TPCCReporter.java 738 2009-12-24 00:19:36Z steveb-oss $
 */
public class TPCCReporter {
  private int count = 0;
  private long timerBase = 0;
  private FileWriter dacapocsv = null;
  private int totalTransactions = 0;
   
  public TPCCReporter() {
  }

  public synchronized void reset(int totalTransactions) {
    this.totalTransactions = totalTransactions;
    count = 0;
    try {
      dacapocsv = new FileWriter(System.getProperty("dacapo.latency.csv"));
      dacapocsv.write("# thread ID, start nsec, end nsec"+System.lineSeparator());
    } catch (Exception e) {
      System.out.println("Failed trying to create latency stats: "+e);
    }
  }

  public synchronized void resetTimerBase() {
    timerBase = System.nanoTime();
  }

  public void close() {
    try {
      dacapocsv.close();
    } catch (Exception e) {
      System.out.println("Failed trying to close latency stats: "+e);
    }  
  }

  public synchronized void done(int tid, long start, long end) {
    count++;
    int tenPercent = totalTransactions / 10;
    if (count % tenPercent == 0) {
      System.out.print((count / tenPercent)+"0% ");
    }
    long s = start - timerBase;
    long e = end - timerBase;
    String str = Integer.toString(tid)+", "+Long.toString(s)+", "+Long.toString(e)+System.lineSeparator();
    try {
      dacapocsv.write(str);
    } catch (Exception ex) {
      System.out.println("Failed trying to write latency stats: "+ex);
    }
  }
}
