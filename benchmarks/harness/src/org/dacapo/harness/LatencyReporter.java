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
import java.util.stream.IntStream;
import java.lang.reflect.Method;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.NoSuchMethodException;
import java.util.concurrent.atomic.AtomicInteger;

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

  private int id;
  private int idxOffset;
  private int idx;
  private int next_idx;
  private double max;
  private static int stride;

  static float[] txbegin;
  static float[] txend;
  static int[] txowner;
  static long requestsStarted;
  static long requestsFinished;
  private static AtomicInteger globalIdx = new AtomicInteger(0);
  static double fileMax;
  static LatencyReporter[] reporters;
  private static long timerBase = 0;  // used to improve precision (when using floats)
  private static Callback callback = null;

  public LatencyReporter(int threadID, int threads, int transactions, int stride) {
    id = threadID;
    idx = 0;
    next_idx = 0;
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
      txowner = new int[transactions];
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
    if (next_idx % stride == 0) {
      next_idx = inc();
    }
    idx = next_idx++;
    if (idx < txbegin.length) {
      startIdx(idx, id);
    }
    return idx;
  }
  private static void startIdx(int index, int threadID) {
    if (callback != null)
      callback.requestStart(index);
    long start = (System.nanoTime() - timerBase)/NS_COARSENING;
    txbegin[index] = (float) start;
    txend[index] = -1;
    txowner[index] = threadID;
  }

  private static int inc() {
    return globalIdx.getAndAdd(stride);
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

  /**
   * Swap elements of txbegin, txend and txowner at
   * indexes i and j.  This allows a sort based on 
   * txbegin with elements of all three arrays retaining
   * the same relative order.
   * 
   * @param i
   * @param j
   */
  private static void swap(int i, int j) {
    float tmp = txbegin[i];
    txbegin[i] = txbegin[j];
    txbegin[j] = tmp;

    tmp = txend[i];
    txend[i] = txend[j];
    txend[j] = tmp;

    int tmpi = txowner[i];
    txowner[i] = txowner[j];
    txowner[j] = tmpi;
  }

  /**
   * Partition sub-array of txbegin using Hoare-style partitioning,
   * and return the pivot.
   * 
   * @param low index of sub-array start
   * @param high index of sub-array end
   * @return index of pivot
   */
  private static int partition(int low, int high) {
    float pivot = txbegin[(low + high) / 2];
    int i = low - 1;
    int j = high + 1;

    while (true) {
      while (txbegin[++i] < pivot);
      while (txbegin[--j] > pivot);
      if (i >= j) {
        return j;
      }
      swap(i, j);
    }
  }

  /**
   * Quicksort of sub-array of txbegin, moving txend and
   * txowner elements in step, so all three arrays are
   * coherently sorted.
   * 
   * @param low index of sub-array start
   * @param high index of sub-array end
   */
  private static void sort(int low, int high) {
    if (low < high) {
      int pi = partition(low, high);
      sort(low, pi);
      sort(pi + 1, high);
    }
  }

  /**
   * Sort of sub-array of txbegin, moving txend and
   * txowner elements in step, so all three arrays are
   * coherently sorted.
   */
  private static void sortEvents() {
    sort(0, txbegin.length - 1);
  }

  private static float smoothedStart(int window, int event) {
    int start = event - (window / 2);
    start = start < 0 ? 0 : start;
    int end = event + (window / 2);
    end = end >= txbegin.length ? txbegin.length - 1 : end;
    float elapsed = txend[end] - txbegin[start];
    float interval = elapsed / (1 + end - start);
    return txbegin[start] + ((event - start) * interval);
  }

  /**
   * Apply a smoothing function to start times, using a sliding window of
   * N events, where N reflects the average number of events that cover
   * a window of the specified size.
   */
  private static float[] smoothedStartEvents(int windowus) {
    int events = txbegin.length;
    double elapsed = txend[events - 1] - txbegin[0];
    int window = (int) (events * ((long) windowus * 1000L / elapsed)); // nanoseconds
    float[] smoothed = new float[events];

    int bigger = 0;
    for(int i = 0; i < events; i++) {
      smoothed[i] = smoothedStart(window, i);
      float tmp = smoothedStart(window/2, i);
      if (smoothed[i] < tmp) {
        bigger++;
      }
    }
    return smoothed;
  }

  /**
   * Apply a smoothing function to start times, using a sliding window of
   * specified microseconds.
   */
  private static float[] smoothedStartTime(int windowus) {
    int start = 0;
    int end = 0;

    int events = txbegin.length;
    float[] smoothed = new float[events];
    float halfWindow = ((long) windowus * 1000L / 2); // nano seconds

    for(int i = 0; i < events; i++) {
      float startns = txbegin[i] - halfWindow;
      while (txbegin[start] < startns && start < (events - 1)) // find the event marking the start of the window
        start++;
      float endns = txbegin[i] + halfWindow;
      while (txbegin[end] < endns && end < (events - 1)) // find the event marking the end of the window
        end++;

      float interval = (txbegin[end] - txbegin[start])/(end - start); // average event interval within the window

      smoothed[i] = txbegin[start] + ((i - start) * interval);
    }
    return smoothed;
  }

  private static void meteredLatency(int[] latency, int windowus, boolean timed) {
    int events = txbegin.length;

    float[] smoothed = timed ? smoothedStartTime(windowus) : smoothedStartEvents(windowus);

    for(int i = 0; i < events; i++) {
      int actual = (int) ((txend[i] - txbegin[i]) / 1000); // usec
      int synth = (int) ((txend[i] - smoothed[i]) / 1000); // usec
      latency[i] = (synth > actual) ? synth : actual;
    }
  }

  private static void meteredLatency(String baseLatencyFileName, boolean dumpLatencyCSV, boolean dumpLatencyHDR, int iteration, int[] latency, int windowus, String desc) {
    meteredLatency(latency, windowus, true);

    if (dumpLatencyCSV)
      dumpLatencyCSV(latency, txbegin, txowner, desc, baseLatencyFileName, iteration);
    if (dumpLatencyHDR)
      dumpLatencyHDR(latency, txbegin, desc, baseLatencyFileName, iteration);
    printLatency(latency, txbegin, txbegin.length, desc, iteration);
  }

  private static void dumpSmoothingCSV(String baseLatencyFileName, int iteration, int[] latency, int elapsedus, boolean timed) {
    int limitus = elapsedus * 2;

    String filename = baseLatencyFileName+"-usec-metered-"+(timed ? "time-" : "events-")+(iteration-1)+".csv";
    try {
      File file = new File(filename);
      BufferedWriter latencyFile = new BufferedWriter(new FileWriter(file));

      String header = "# window us, 50";
      int precision = 10;
      String precstr = "90";
      while (precision <= TAIL_PRECISION) {
        header += ", "+precstr;
        precision *= 10;
        if (precstr.equals("90"))
          precstr = "99";
        else
          precstr += precstr.equals("99") ? ".9" : "9";
      }

      latencyFile.write(header + System.lineSeparator());

      double step = Math.pow(2.0, 0.125); // exponential steps in 1/8 of powers of two
      for (double windowus = 100; windowus <= limitus; windowus *= step) {
        meteredLatency(latency, (int) windowus, timed);
        Arrays.sort(latency);

        String row = ""+windowus;
        row += ", "+latency(latency, 50, 100);
        precision = 10;
        while (precision <= TAIL_PRECISION) {
          row += ", "+latency(latency, 1, precision);
          precision *= 10;
        }
        latencyFile.write(row + System.lineSeparator());
      }

      latencyFile.close();
    } catch (IOException e) {
      System.err.println("Failed to write latency file '"+filename+"'"+System.lineSeparator()+e);
    }
  }


  public static void reportLatency(String baseLatencyFileName, boolean dumpLatencyCSV, boolean dumpLatencyHDR, boolean dumpLatencySmoothing, int iteration) {
    if (timerBase != 0) {
      sortEvents();
      int events = txbegin.length;
      printRequestTime(events);

      // raw latency numbers
      int[] latency = new int[events];
      float end = 0;
      for(int i = 0; i < events; i++) {
        latency[i] = (int) ((txend[i] - txbegin[i])/1000);
        if (txend[i] > end) {
          end = txend[i];
        }
      }
      if (dumpLatencyCSV)
        dumpLatencyCSV(latency, txbegin, txowner, "simple", baseLatencyFileName, iteration);
      if (dumpLatencyHDR)
        dumpLatencyHDR(latency, txbegin, "simple", baseLatencyFileName, iteration);
      printLatency(latency, txbegin, events, "simple", iteration);

      meteredLatency(baseLatencyFileName, dumpLatencyCSV, dumpLatencyHDR, iteration, latency, 100000, "metered 100ms smoothing");

      int elapsedus = (int) ((txend[events - 1] - txbegin[0])/1000.0);
      int limitus = elapsedus * 10;
      meteredLatency(baseLatencyFileName, dumpLatencyCSV, dumpLatencyHDR, iteration, latency, limitus, "metered full smoothing");


      if (dumpLatencySmoothing) {
        dumpSmoothingCSV(baseLatencyFileName, iteration, latency, elapsedus, true);
      }
    }
  }


  private static int latency(int[] latency, int numerator, int denominator) {
    int usecs = (latency[latency.length - 1 - (latency.length * numerator) / denominator]);
    return usecs;
  }

  public static void printRequestTime(int events) {
    String report = "===== DaCapo processed "+events+" requests ";
    long ms = (requestsFinished - requestsStarted)/1000000;
    report += "in "+ms+" msec, ";
    long rps = (1000L * events) / ms;
    report += rps+" requests per second =====";
    System.out.println(report);
  }

  public static void printLatency(int[] latency, float[] txbegin, int events, String kind, int iteration) {
    Arrays.sort(latency);
    String report = "===== DaCapo tail latency, "+kind+": ";
    report += "50% " + latency(latency, 50, 100)+" usec";
    int precision = 10;
    String precstr = "90";
    while (precision <= TAIL_PRECISION) {
      report += ", " + precstr + "% " + latency(latency, 1, precision)+" usec";
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

  private static void dumpLatencyCSV(int[] latency, float[] txbegin, int[] txowner, String kind, String baseFilename, int iteration) {
    String filename = baseFilename+"-usec-"+kind.replace(" ", "-")+"-"+(iteration-1)+".csv";
    try {
      File file = new File(filename);
      BufferedWriter latencyFile = new BufferedWriter(new FileWriter(file));
      for (int i = 0; i < latency.length; i++) {
        int start = (int) (txbegin[i]/1000);
        latencyFile.write(start+", "+(start+latency[i])+", "+txowner[i]+System.lineSeparator());
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
    globalIdx.set(0);
    System.out.println("Starting "+txbegin.length+" requests...");
    if (callback != null) callback.requestsStarting();
    requestsStarted = System.nanoTime();
  }

  public static void requestsFinished() {
    requestsFinished = System.nanoTime();
    System.out.println("Completed requests");
    if (callback != null) callback.requestsFinished();
  }

}
