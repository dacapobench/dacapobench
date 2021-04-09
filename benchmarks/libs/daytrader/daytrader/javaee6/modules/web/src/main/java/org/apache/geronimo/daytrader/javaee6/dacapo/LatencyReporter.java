package org.apache.geronimo.daytrader.javaee6.dacapo;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class LatencyReporter {
  static final int NS_COARSENING = 1; // measure at this precision

  private static double[] latency;
  private static Integer globalIdx = 0;
  private static long timerBase;

  static void initialize(int logNumSessions) {
    int operations = getOperations(logNumSessions);
    latency = new double[operations];
    timerBase = System.nanoTime();
    globalIdx = 0;
  }

  static int start() {
    int index = 0;
    synchronized (globalIdx) {
      index = globalIdx++;
    }
    double start = (System.nanoTime() - timerBase) / NS_COARSENING;
    latency[index] = (double) -start;
    long start_cast = Double.valueOf(-latency[index]).longValue();
    if (start_cast != start) {
      System.err.println("WARNING: Timing precision error: " + start + " != " + start_cast);
    }
    return index;
  }

  static void end(int index) {
    long end = (System.nanoTime() - timerBase) / NS_COARSENING;
    latency[index] += (double) end;
  }

  static void report() {
    try {
      File file = new File(System.getProperty("dacapo.latency.file"));
      BufferedWriter output = new BufferedWriter(new FileWriter(file));
      output.write(Integer.toString(latency.length)+System.lineSeparator());
      for (int i = 0; i < latency.length; i++) {
        output.write(Double.toString(latency[i])+System.lineSeparator());
      }
      output.close();
    } catch (IOException ioe) {
      ioe.printStackTrace();
    }
  }

  private static int getOperations(int logNumSessions) {
    int rtn = 0;
    try {
      FileReader operationsFile = new FileReader(new File(System.getProperty("dacapo.daytrader.ops")));
      BufferedReader br = new BufferedReader(operationsFile);
      String s;
      while ((s = br.readLine()) != null) {
        String[] l = s.split(", ");
        if (Integer.parseInt(l[0]) == logNumSessions) {
          rtn = Integer.parseInt(l[1]);
          break;
        }
      }
      br.close();
    } catch (Exception e) {
        System.err.println("Failed to open operations.csv: "+e);
    }
    return rtn;
  }
}