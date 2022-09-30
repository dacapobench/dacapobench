package org.apache.geronimo.daytrader.javaee6.dacapo;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import java.lang.ClassLoader;
import java.net.URLClassLoader;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

public class LatencyReporter {
  private static Method dacapoInitializeLR;
  private static Method dacapoRequestsReset;
  private static Method dacapoRequestsStarting;
  private static Method dacapoRequestsFinished;
  private static Method dacapoRequestStart;
  private static Method dacapoRequestEnd;

  static final int NS_COARSENING = 1; // measure at this precision

  static void initialize(int threads, int logNumSessions) {
    int transactions = getOperations(logNumSessions);

    /* Get references to each of the latency reporter methods */
    try {
      Class<?> clazz = Class.forName("org.dacapo.harness.LatencyReporter",
          true, ClassLoader.getSystemClassLoader());
      dacapoInitializeLR = clazz.getMethod("initialize", int.class, int.class, int.class);
      dacapoRequestStart = clazz.getDeclaredMethod("start", int.class);
      dacapoRequestEnd = clazz.getMethod("end", int.class);
      dacapoRequestsStarting = clazz.getDeclaredMethod("requestsStarting", null);
      dacapoRequestsFinished = clazz.getDeclaredMethod("requestsFinished", null);
    } catch (ClassNotFoundException e) {
      System.err.println("Failed to resolve DaCapo latency reporter class: "+e);
    } catch (NoSuchMethodException e) {
      System.err.println("Failed to resolve methods within DaCapo latency reporter: "+e);
    }

    /* Initialize the latency reporter */
    try {
      dacapoInitializeLR.invoke(null, transactions, threads, 1);
    } catch (IllegalAccessException e) {
      System.err.println("Failed to access DaCapo latency reporter: "+e);
    } catch (InvocationTargetException e) {
      System.err.println("Failed to invoke LatencyReporter.initialize(): "+e);
    }
  }

  static void starting() {
    try {
      dacapoRequestsStarting.invoke(null);
    } catch (IllegalAccessException e) {
      System.err.println("Failed to access DaCapo latency reporter: "+e);
    } catch (InvocationTargetException e) {
      System.err.println("Failed to invoke LatencyReporter.requestsStarting(): "+e);
    }
  }

  static void finished() {
    try {
      dacapoRequestsFinished.invoke(null);
    } catch (IllegalAccessException e) {
      System.err.println("Failed to access DaCapo latency reporter: "+e);
    } catch (InvocationTargetException e) {
      System.err.println("Failed to invoke LatencyReporter.requestsFinished(): "+e);
    }
  }

  static int start(int threadID) {
    try {
      Object idx = dacapoRequestStart.invoke(null, threadID);
      return (Integer) idx;
    } catch (IllegalAccessException e) {
      System.err.println("Failed to access DaCapo latency reporter: "+e);
    } catch (InvocationTargetException e) {
      System.err.println("Failed to invoke LatencyReporter.start("+threadID+"): "+e);
    }
    return -1;
  }

  static void end(int threadID) {
    try {
      dacapoRequestEnd.invoke(null, threadID);
    } catch (IllegalAccessException e) {
      System.err.println("Failed to access DaCapo latency reporter: "+e);
    } catch (InvocationTargetException e) {
      System.err.println("Failed to invoke LatencyReporter.end("+threadID+"): "+e);
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
    if (rtn == 0) {
      System.err.println("ERROR: Failed to find operation count for 2^"+logNumSessions+" operations");
      System.exit(-1);
    }
    return rtn;
  }
}