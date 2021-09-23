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
import java.io.IOException;
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
 * date:  $Date: 2009-12-24 11:19:36 +1100 (Thu, 24 Dec 2009) $
 * id: $Id: TestHarness.java 738 2009-12-24 00:19:36Z steveb-oss $
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

    setBuildInfo();  // set BuildVersion and BuildNickName.
    if (BuildVersion.contains("git")) {
      System.err.println(
        "--------------------------------------------------------------------------------\n" +
        "IMPORTANT NOTICE:  This is NOT a release build of the DaCapo suite.\n" +
        "Since it is not an official release of the DaCapo suite, care must be taken when\n" +
        "using the suite, and any use of the build must be sure to note that it is not an\n" +
        "offical release, and should note the relevant git hash.\n" +
        "\n" +
        "Feedback is greatly appreciated.   The preferred mode of feedback is via github.\n"+
	    "Please use our github page to create an issue or a pull request.\n"+
	    "    https://github.com/dacapobench/dacapobench.\n"+
        "--------------------------------------------------------------------------------\n"
      );
    }
    try {
      commandLineArgs = new CommandLineArgs(args);

      File scratch = new File(commandLineArgs.getScratchDir());
      makeCleanScratch(scratch);
      File data = new File(Data.getLocation());

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
          String cnf = "META-INF/cnf/" + bm + ".cnf";
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
          try {
            runBenchmark(scratch, data, bm, harness);
          } catch (FileNotFoundException e) {
            System.err.printf("ERROR: The following file used by size '%s' could not be found: %s\n", size, e.getMessage());
            System.err.printf("Please check that you have downloaded the required data for this size, " +
                              "and have installed it correctly under %s/%s\n", data.getAbsolutePath(), bm);
            System.err.printf("Note: the directory for big data is currently set to: %s\n", data.getAbsolutePath());
            System.err.printf("To change this path, please run `java -jar %s -i <new_data_dir>`.\n", args[0]);
            System.exit(-1);
          }
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
   * @param data
   * @param bm
   * @param harness
   * @throws NoSuchMethodException
   * @throws InstantiationException
   * @throws IllegalAccessException
   * @throws InvocationTargetException
   * @throws Exception
   */
  private static void runBenchmark(File scratch, File data, String bm, TestHarness harness) throws NoSuchMethodException, InstantiationException, IllegalAccessException,
      InvocationTargetException, Exception {
    harness.config.printThreadModel(System.out, commandLineArgs.getSize(), commandLineArgs.getVerbose());

    Constructor<?> cons = harness.findClass().getConstructor(new Class[] { Config.class, File.class, File.class });

    Benchmark b = (Benchmark) cons.newInstance(new Object[] { harness.config, scratch, data });

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

  public static String getManifestAttribute(String key) throws IOException {
    String url = TestHarness.class.getProtectionDomain().getCodeSource().getLocation().getFile();
    JarFile jarFile = new JarFile(url.replace("!/harness", "").replace("file:", ""));
    Manifest manifest = jarFile.getManifest();
    Attributes attributes = manifest.getMainAttributes();
    return attributes.get(new Attributes.Name(key)).toString();
  }

  private static void setBuildInfo() {
    try {
      String nickname = getManifestAttribute(BUILD_NICKNAME);
      String version = getManifestAttribute(BUILD_VERSION);

      BuildNickName = nickname;
      BuildVersion = version;
    } catch (Exception e) {
      BuildNickName = "Unknown";
      BuildVersion = "unknown";
    }
  }
}