/*
 * Copyright (c) 2006, 2009 The Australian National University.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License v2.0.
 * You may obtain the license at
 * 
 *    http://www.opensource.org/licenses/apache2.0.php
 */
package org.dacapo.harness;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.jar.JarFile;
import java.util.Locale;

import org.dacapo.parser.Config;

/**
 * Main class for the Dacapo benchmark suite. Locates the configuration file for
 * the specified benchmark, interprets command line arguments, and invokes the
 * benchmark-specific harness class.
 * 
 * @date $Date: 2009-12-24 11:19:36 +1100 (Thu, 24 Dec 2009) $
 * @id $Id: TestHarness.java 738 2009-12-24 00:19:36Z steveb-oss $
 */
public class TestHarness {

  public static final String BUILD_NICKNAME = "Specification-Version";
  public static final String BUILD_VERSION = "Implementation-Version";

  // these hold the build nick name and version strings respectively
  private static String BuildNickName;
  private static String BuildVersion;

  private final Config config;
  private static CommandLineArgs commandLineArgs;

  public static String getBuildNickName() {
    return BuildNickName;
  }

  public static String getBuildVersion() {
    return BuildVersion;
  }

  /**
   * Calculates coefficient of variation of a set of longs (standard deviation
   * divided by mean).
   * 
   * @param times Array of input values
   * @return Coefficient of variation
   */
  public static double coeff_of_var(long[] times) {
    double n = times.length;
    double sum = 0.0;
    double sum2 = 0.0;

    for (int i = 0; i < times.length; i++) {
      double x = times[i];
      sum += x;
      sum2 += x * x;
    }

    double mean = sum / n;
    double sigma = Math.sqrt(1.0 / n * sum2 - mean * mean);

    return sigma / mean;
  }

  public static void main(String[] args) {
    // force the locale so that we don't have any character set issues
    // when generating output for the digests.
    Locale.setDefault(new Locale("en", "AU"));

    /* All benchmarks run headless */
    System.setProperty("java.awt.headless", "true");

    try {
      commandLineArgs = new CommandLineArgs(args);

      File scratch = new File(commandLineArgs.getScratchDir());
      makeCleanScratch(scratch);

      // this is not right
      Benchmark.setCommandLineOptions(commandLineArgs);
      try {
        Config.setThreadCountOverride(Integer.parseInt(commandLineArgs.getThreadCount()));
      } catch (RuntimeException re) {
      }

      // now get the benchmark names and run them
      for (String bm : commandLineArgs.benchmarks()) {
        // check if it is a benchmark name
        // name of file containing configurations
        InputStream ins = null;
        if (commandLineArgs.getCnfOverride() == null) {
          String cnf = "cnf/" + bm + ".cnf";
          ins = TestHarness.class.getClassLoader().getResourceAsStream(cnf);
          if (ins == null) {
            System.err.println("Unknown benchmark: " + bm);
            System.exit(20);
          }
        } else {
          String cnf = commandLineArgs.getCnfOverride();
          try {
            ins = new FileInputStream(cnf);
          } catch (FileNotFoundException e) {
            System.err.println("Count not find cnf file: '" + cnf + "'");
            System.exit(20);
          }
        }

        TestHarness harness = new TestHarness(ins);

        String size = commandLineArgs.getSize();

        int factor = 0;
        int limit = harness.config.getThreadLimit(size);

        try {
          factor = Integer.parseInt(commandLineArgs.getThreadFactor());
          if (0 < factor && harness.config.getThreadModel() == Config.ThreadModel.PER_CPU)
            harness.config.setThreadFactor(size, factor);
        } catch (RuntimeException re) {
        }

        if (!harness.isValidSize(size)) {
          System.err.println("No configuration size, " + size + ", for benchmark " + bm + ".");
        } else if (factor != 0 && harness.config.getThreadModel() != Config.ThreadModel.PER_CPU) {
          System.err.println("Can only set the thread factor for per_cpu configurable benchmarks");
        } else if (!harness.isValidThreadCount(size) && (harness.config.getThreadCountOverride() > 0 || factor > 0)) {
          System.err.println("The specified number of threads (" + harness.config.getThreadCount(size) + ") is outside the range [1,"
              + (limit == 0 ? "unlimited" : "" + limit) + "]");
        } else if (commandLineArgs.getInformation()) {
          harness.bmInfo(size);
        } else if (commandLineArgs.getSizes()) {
          harness.bmSizes();
        } else {
          if (!harness.isValidThreadCount(size)) {
            System.err.println("The derived number of threads (" + harness.config.getThreadCount(size) + ") is outside the range [1,"
                + (limit == 0 ? "unlimited" : "" + limit) + "]; rescaling to match thread limit.");
            harness.config.setThreadCountOverride(harness.config.getThreadLimit(size));
          }

          harness.dump(commandLineArgs.getVerbose());
          runBenchmark(scratch, bm, harness);
        }
      }
    } catch (Exception e) {
      System.err.println(e);
      e.printStackTrace();
      System.exit(-1);
    }
  }

  public static void makeCleanScratch(File scratch) {
    rmdir(scratch);
    scratch.mkdir();
  }

  private boolean isValidSize(String size) {
    return size != null && config.getSizes().contains(size);
  }

  private boolean isValidThreadCount(String size) {
    return config.getThreadLimit(size) == 0 || config.getThreadCount(size) <= config.getThreadLimit(size);
  }

  /**
   * @param scratch
   * @param bm
   * @param harness
   * @param c
   * @throws NoSuchMethodException
   * @throws InstantiationException
   * @throws IllegalAccessException
   * @throws InvocationTargetException
   * @throws Exception
   */
  private static void runBenchmark(File scratch, String bm, TestHarness harness) throws NoSuchMethodException, InstantiationException, IllegalAccessException,
      InvocationTargetException, Exception {
    harness.config.printThreadModel(System.out, commandLineArgs.getSize(), commandLineArgs.getVerbose());

    Constructor<?> cons = harness.findClass().getConstructor(new Class[] { Config.class, File.class });

    Benchmark b = (Benchmark) cons.newInstance(new Object[] { harness.config, scratch });

    boolean valid = true;
    Callback callback = commandLineArgs.getCallback();
    callback.init(harness.config);

    do {
      valid = b.run(callback, commandLineArgs.getSize()) && valid;
    } while (callback.runAgain());
    b.cleanup();

    if (!valid) {
      System.err.println("Validation FAILED for " + bm + " " + commandLineArgs.getSize());
      if (!commandLineArgs.getIgnoreValidation())
        System.exit(-2);
    }
  }

  private static void rmdir(File dir) {
    String[] files = dir.list();
    if (files != null) {
      for (int f = 0; f < files.length; f++) {
        File file = new File(dir, files[f]);
        if (file.isDirectory())
          rmdir(file);
        if (!file.delete())
          System.err.println("Could not delete " + files[f]);
      }
    }
  }

  private void bmInfo(String size) {
    config.describe(System.err, size);
  }

  private void bmSizes() {
    config.describeSizes(System.err);
  }

  private void dump(boolean verbose) {
    if (verbose) {
      System.err.println("Class name: " + config.className);

      System.err.println("Configurations:");
      config.describe(System.err, commandLineArgs.getSize());
    }
  }

  private TestHarness(InputStream stream) {
    config = Config.parse(stream);
    if (config == null)
      System.exit(-1);
  }

  private Class<?> findClass() {
    try {
      return Class.forName(config.className);
    } catch (ClassNotFoundException e) {
      System.err.println(e);
      e.printStackTrace();
      System.exit(-1);
      return null; // not reached
    }
  }

  {
    try {
      String url = TestHarness.class.getProtectionDomain().getCodeSource().getLocation().getFile();
      JarFile jarFile = new JarFile(url.replace("!/harness", "").replace("file:", ""));

      Manifest manifest = jarFile.getManifest();
      Attributes attributes = manifest.getMainAttributes();

      String nickname = attributes.get(new Attributes.Name(BUILD_NICKNAME)).toString();
      String version = attributes.get(new Attributes.Name(BUILD_VERSION)).toString();

      BuildNickName = nickname;
      BuildVersion = version;
    } catch (Exception e) {
      BuildNickName = "Unknown";
      BuildVersion = "unknown";
    }
  }
}