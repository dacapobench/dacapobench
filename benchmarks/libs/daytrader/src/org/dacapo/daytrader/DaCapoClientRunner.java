/*
 * Copyright (c) 2009 The Australian National University.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License v2.0.
 * You may obtain the license at
 * 
 *    http://www.opensource.org/licenses/apache2.0.php
 */
package org.dacapo.daytrader;

import java.lang.ProcessBuilder;
import java.lang.Process;
import java.io.File;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import java.util.Set;
import java.util.Collection;

/**
 * @date $Date: 2009-12-24 11:19:36 +1100 (Thu, 24 Dec 2009) $
 * @id $Id: DaCapoClientRunner.java 738 2009-12-24 00:19:36Z steveb-oss $
 */
public class DaCapoClientRunner {

  private static String car = null;
  private static String gero = null;

  public static void initialize(String carName, String size, int numThreads, boolean useBeans) {
    try {

      car = carName;

      gero = System.getProperty("org.apache.geronimo.home.dir");
      ProcessBuilder pb = new ProcessBuilder("java", "-jar", "-Dkaraf.startLocalConsole=false",gero + "/bin/client.jar", car, "-i", "-t", numThreads + "", "-s", size, useBeans ? "-b" : "");
      Process p = pb.start();
      p.waitFor();

    } catch (Exception e) {
      System.err.print("Exception initializing client: " + e.toString());
      e.printStackTrace();
      System.exit(-1);
    }
  }

  public static void runIteration(String size, int numThreads, boolean useBeans) {

    try {

      ProcessBuilder pb = new ProcessBuilder("java", "-jar", "-Dkaraf.startLocalConsole=false", gero + "/bin/client.jar", car, "-t", numThreads + "", "-s", size, useBeans ? "-b" : "");
      Process p = pb.start();
      p.waitFor();

      BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
      while (stdInput.ready()) { //While there's something in the buffer
        //read&print - replace with a buffered read (into an array) if the output doesn't contain CR/LF
        System.out.println(stdInput.readLine());
      }

    } catch (Exception e) {
      System.err.print("Exception running client iteration: " + e.toString());
      e.printStackTrace();
    }
  }
}
