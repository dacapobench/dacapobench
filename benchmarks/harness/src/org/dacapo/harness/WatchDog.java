/*
 * Copyright (c) 2006, 2009, 2020 The Australian National University.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License v2.0.
 * You may obtain the license at
 * 
 *    http://www.opensource.org/licenses/apache2.0.php
 */
package org.dacapo.harness;

/**
 * A watchdog timer which will terminate the JVM if it is still running 
 * after the specified time has elapsed.  It may be set by the user at
 * the command line or by a specific benchmark.  The command line setting
 * takes precedence.
 * 
 * @param seconds Terminate the JVM after this many seconds have elapsed
 * @param actor A string representing the instigator of the call.
 * @param usage A string reflecting usage (printed to the console)
 */
public class WatchDog {

  private static String instigator = null;

  public static void set(int seconds, String actor, String usage) {
    if (instigator != null) {
      System.err.println("WARNING: Attempt by " + actor + " to set the watchdog timer is being ignored since it was already set by " + instigator);
    } else {
      instigator = actor;
      System.out.println("NOTE: Starting watchdog timer with timeout of "+seconds+" seconds. " + usage);
      new Thread(() -> {
        try {
          Thread.sleep(seconds*1000);
        } catch (InterruptedException e) {
        } finally {
          System.err.println();
          System.err.println("ERROR: Watchdog timer set for " + seconds + " seconds by " + actor + " has expired. " + usage + " Quitting abruptly.");
          System.err.println();
          System.err.flush();
          System.exit(-1);
        }
      }).start();
    }
  }
}
