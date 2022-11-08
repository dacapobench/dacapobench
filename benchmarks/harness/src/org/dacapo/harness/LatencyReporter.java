/*
 * Copyright (c) 2006, 2009, 2020 The Australian National University.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License v2.0.
 * You may obtain the license at
 * 
 *    http://www.opensource.org/licenses/apache2.0.php
 */
package org.dacapo.harness;

import java.util.Arrays;

import java.lang.reflect.Method;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.NoSuchMethodException;

import org.HdrHistogram.*;

/**
 * Thread-local latency reporter used to generate tail-latency stats in
 * a resource-sensitive, contention-free, statistically-robust way.
 */
public class LatencyReporter {
  static final int TAIL_PRECISION = 10000;  // report tail to 1/TAIL_PRECISION
  static final int LATENCY_BUFFER_SIZE = 1000 * TAIL_PRECISION;
  static final int NS_COARSENING = 1;   // measure at this precision
  static final int US_DIVISOR = 1000/NS_COARSENING;
  
  private int idxOffset;
  private int idx;
  private double max;
  private static int stride;

  static float[] txbegin;
  static float[] txend;
  private static Integer globalIdx = 0;
  static double fileMax;
  static LatencyReporter[] reporters;
  private static long timerBase = 0;  // used to improve precision (when using floats)
  private static Callback callback = null;

  public LatencyReporter(int threadID, int threads, int transactions, int stride) {
    idx = -1;
    max = 0;
    reporters[threadID] = this;
  }

  public static void setCallback(Callback cb) {
    callback = cb;
  }
  
  public static void initialize(int transactions, int threads) {
    initialize(transactions, threads, 1);
  }

  public static void initialize(int transactions, int threads, int _stride) {
    stride = _stride;
    timerBase = System.nanoTime();
    if (transactions > LATENCY_BUFFER_SIZE) {
      System.err.println("Too many transactions. "+transactions+" > LATENCY_BUFFER_SIZE ("+LATENCY_BUFFER_SIZE+")");
      java.lang.Thread.dumpStack();
      System.exit(-1);
    } else {
      txbegin = new float[transactions];
      txend = new float[transactions];
      reporters = new LatencyReporter[threads];
      for (int i = 0; i < threads; i++) {
        reporters[i] = new LatencyReporter(i, threads, transactions, stride);
      }
    }
  }

  public static LatencyReporter[] getLatencyReporters() {
    return reporters;
  }

  /**
   * Start a request
   * 
   *   * @param threadID the thread in which the request ran.
   * 
   * @return the index
   */
  public static int start(int threadID) {
    return reporters[threadID].start();
  }
  public int start() {
    idx++;
    if (idx % stride == 0)
      idx = inc();
    if (idx < txbegin.length)
      startIdx(idx);
    return idx;
  }
  private static void startIdx(int index) {
    if (callback != null) 
      callback.requestStart(index);
    long start = (System.nanoTime() - timerBase)/NS_COARSENING;
    txbegin[index] = (float) start;
    txend[index] = -1;
  }

  private static int inc() {
    int rtn;
    synchronized (globalIdx) {
       rtn = globalIdx += stride;
    }
    return rtn;
  }

  /**
   * A request has just completed.
   * 
   * @param threadID the thread in which the request ran.
   */
  public static void end(int threadID) {
    reporters[threadID].end();
  }
  public void end() {
    endIdx(idx);
  }

  /**
   * A request has just completed.
   * 
   * @param index the global index for the request that completed.
   */
  public static float endIdx(int index) {
    long end = (System.nanoTime() - timerBase)/NS_COARSENING;
    txend[index] = (float) end;

    if (callback != null) callback.requestEnd(index);
    return txend[index];
  }

  public static void reportLatency(String baseLatencyFileName, boolean dumpLatencyCSV, boolean dumpLatencyHDR, int iteration) {
    if (timerBase != 0) {
      int events = txbegin.length;

      // raw latency numbers
      int[] latency = new int[events];
      for(int i = 0; i < events; i++) {
        latency[i] = (int) ((txend[i] - txbegin[i])/1000);
      }
      if (dumpLatencyCSV)
        dumpLatencyCSV(latency, txbegin, "simple", baseLatencyFileName, iteration);
      if (dumpLatencyHDR)
        dumpLatencyHDR(latency, txbegin, "simple", baseLatencyFileName, iteration);
      printLatency(latency, txbegin, events, "simple", iteration);

      // synthetically metered --- each query start is evenly spaced, so delays will compound
      float[] sorted = Arrays.copyOf(txbegin, events);
      Arrays.sort(sorted);
      double len = sorted[sorted.length-1]-sorted[0];
      double synthstart = 0;
      for(int i = 0; i < events; i++) {
        int pos = Arrays.binarySearch(sorted, txbegin[i]);
        synthstart = sorted[0] + (len*(double) pos / (double) txbegin.length);
        int actual = (int) ((txend[i] - txbegin[i])/1000);
        int synth = (int) ((txend[i] - synthstart)/1000);
        latency[i] = (synth > actual) ? synth : actual;
      }
      if (dumpLatencyCSV)
        dumpLatencyCSV(latency, txbegin, "metered", baseLatencyFileName, iteration);
      if (dumpLatencyHDR)
        dumpLatencyHDR(latency, txbegin, "metered", baseLatencyFileName, iteration);
      printLatency(latency, txbegin, events, "metered", iteration);
    }
  }

  private static String latency(int[] latency, int numerator, int denominator) {
    int usecs = (latency[latency.length - 1 - (latency.length * numerator) / denominator]);
    return ""+usecs+" usec";
  }

  public static void printLatency(int[] latency, float[] txbegin, int events, String kind, int iteration) {
    Arrays.sort(latency);
    String report = "===== DaCapo "+kind+" tail latency: ";
    report += "50% " + latency(latency, 50, 100);
    int precision = 10;
    String precstr = "90";
    while (precision <= TAIL_PRECISION) {
      report += ", " + precstr + "% " + latency(latency, 1, precision);
      precision *= 10;
      if (precstr.equals("90"))
        precstr = "99";
      else
        precstr += precstr.equals("99") ? ".9" : "9";
    }
    report += ", max "+((int) latency[latency.length-1])+" usec";
    report += ", measured over "+events+" events =====";
    System.out.println(report);
  }

  private static void dumpLatencyCSV(int[] latency, float[] txbegin, String kind, String baseFilename, int iteration) {
    String filename = baseFilename+"-usec-"+kind+"-"+(iteration-1)+".csv";
    try {
      File file = new File(filename);
      BufferedWriter latencyFile = new BufferedWriter(new FileWriter(file));
      for (int i = 0; i < latency.length; i++) {
        int start = (int) (txbegin[i]/1000);
        latencyFile.write(start+", "+(start+latency[i])+System.lineSeparator());
      }
      latencyFile.close();
    } catch (IOException e) {
      System.err.println("Failed to write latency file '"+filename+"'"+System.lineSeparator()+e);
    }
  }

  private static void dumpLatencyHDR(int[] latency, float[] txbegin, String kind, String baseFilename, int iteration) {
    Histogram histogram = new Histogram(60000000, 4);  // 1usec -> 1min, 4 decimal places
    for (int i = 0; i < latency.length; i++)
      histogram.recordValue(latency[i]);

    String filename = baseFilename+"-usec-"+kind+"-"+(iteration-1)+".hdr";
    try {
      HistogramLogWriter hlw = new HistogramLogWriter(filename);
      final double start = txbegin[0]/1000000000;
      final double end = (txbegin[txbegin.length-1]/1000000000)+(latency[txbegin.length-1]/1000000);

      hlw.outputIntervalHistogram(start, end, histogram);
    } catch (IOException e) {
      System.err.println("Failed to write latency file '"+filename+"'"+System.lineSeparator()+e);
    }
  }

  public static void requestsStarting() {
    globalIdx = -stride;
    System.err.println("Starting "+txbegin.length+" requests...");
    if (callback != null) callback.requestsStarting();
  }

  public static void requestsFinished() {
    System.err.println("Completed requests");
    if (callback != null) callback.requestsFinished();
  }

}