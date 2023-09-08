/*
 * Copyright (c) 2006, 2009 The Australian National University.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License v2.0.
 * You may obtain the license at
 * 
 *    http://www.opensource.org/licenses/apache2.0.php
 */
package org.dacapo.harness;

import org.dacapo.harness.CommandLineArgs.Methodology;
import org.dacapo.parser.Config;

/**
 * @date $Date: 2009-12-24 11:19:36 +1100 (Thu, 24 Dec 2009) $
 * @id $Id: Callback.java 738 2009-12-24 00:19:36Z steveb-oss $
 */
public class Callback {

  /**
   * Support for timing methodologies that have timing and warmup runs.
   */
  protected enum Mode {
    WARMUP, TIMING
  };

  protected Mode mode;

  /**
   * The parsed command line arguments
   */
  protected final CommandLineArgs args;

  /**
   * Iterations of the current benchmark completed so far
   */
  protected int iterations;

  /**
   * Times for the last n iterations of the current benchmark
   */
  protected long[] times;

  /**
   * 
   */
  protected long elapsed;

  boolean verbose = false;

  /**
   * Create a new callback.
   * 
   * @param args The parsed command-line arguments.
   */
  public Callback(CommandLineArgs args) {
    this.args = args;
    if (args.getMethodology() == Methodology.CONVERGE) {
      times = new long[args.getWindow()];
    }
    verbose |= args.getDebug();
  }

  public void init(Config config) {
    if (verbose)
      System.out.println("Initializing callback");
    iterations = 0;

    switch (args.getMethodology()) {
    case ITERATE:
      if (args.getIterations() == 1)
        mode = Mode.TIMING;
      else
        mode = Mode.WARMUP;
      break;
    case CONVERGE:
      if (args.getWindow() == 0)
        mode = Mode.TIMING;
      else
        mode = Mode.WARMUP;
    }

    if (times != null)
      for (int i = 0; i < times.length; i++)
        times[i] = 0;
  }

  /**
   * This method governs the benchmark iteration process. The test harness will
   * run the benchmark repeatedly until this method returns 'false'.
   * 
   * The default methodologies consist of 0 or more 'warmup' iterations,
   * followed by a single timing iteration.
   * 
   * @return Whether to run another iteration.
   */
  public boolean runAgain() {
    if (verbose)
      System.out.println("runAgain");
    /* Always quit immediately after the timing iteration */
    if (!isWarmup())
      return false;

    iterations++;
    if (verbose)
      System.out.println("iterations = " + iterations);
    switch (args.getMethodology()) {
    case ITERATE:
      if (iterations == args.getIterations() - 1)
        mode = Mode.TIMING;
      if (verbose)
        System.out.println("mode = " + mode);
      return true;

    case CONVERGE:
      /* If we've exceeded the maximum iterations, exit */
      if (iterations >= args.getMaxIterations()) {
        System.err.println("Benchmark failed to converge.");
        return false;
      }

      /* Maintain the sliding window of execution times */
      times[(iterations - 1) % args.getWindow()] = elapsed;

      /* If we haven't filled the window, repeat immediately */
      if (iterations < args.getWindow())
        return true;

      /* Optionally report on progress towards convergence */
      if (iterations >= args.getWindow() && args.getVerbose()) {
        System.err.printf("Variation %4.2f%% achieved after %d iterations, target = %4.2f%%\n", TestHarness.coeff_of_var(times) * 100, iterations, args
            .getTargetVar() * 100);
      }

      /* Not yet converged, repeat in warmup mode */
      if (TestHarness.coeff_of_var(times) > args.getTargetVar())
        return true;

      /* If we've fallen through to here, we must have converged */
      mode = Mode.TIMING;
      return true;
    }

    // We should never fall through
    assert false;
    return false; // Keep javac happy
  }

  public boolean isWarmup() {
    return mode == Mode.WARMUP;
  }

  /**
   * Start the timer and announce the begining of an iteration
   */
  public void start(String benchmark) {
    start(benchmark, mode == Mode.WARMUP);
  };

  protected void start(String benchmark, boolean warmup) {
    System.err.print("===== DaCapo " + TestHarness.getBuildVersion() + " " + benchmark + " starting ");
    System.err.println((warmup ? ("warmup " + (iterations + 1) + " ") : "") + "=====");
    System.err.flush();
  }

  public void stop(long duration) {
    stop(duration, mode == Mode.WARMUP);
  }

  public void stop(long duration, boolean warmup) {
    elapsed = duration;
  }

  /* Announce completion of the benchmark (pass or fail) */
  public void complete(String benchmark, boolean valid) {
    complete(benchmark, valid, mode == Mode.WARMUP);
  };

  protected void complete(String benchmark, boolean valid, boolean warmup) {
    System.err.print("===== DaCapo " + TestHarness.getBuildVersion() + " " + benchmark);
    if (valid) {
      System.err.print(warmup ? (" completed warmup " + (iterations + 1) + " ") : " PASSED ");
      System.err.print("in " + elapsed + " msec ");
    } else {
      System.err.print(" FAILED " + (warmup ? "warmup " : ""));
    }
    System.err.println("=====");
    System.err.flush();
  }

}
