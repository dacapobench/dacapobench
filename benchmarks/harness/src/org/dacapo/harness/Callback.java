package org.dacapo.harness;

import org.dacapo.harness.CommandLineArgs.Methodology;
import org.dacapo.parser.Config;

public class Callback {
  
  /**
   * Support for timing methodologies that have timing and
   * warmup runs.
   */
  protected enum Mode { WARMUP, TIMING };
  
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
   * The start time of the most recent benchmark run.
   */
  protected long timer;
  
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
    if (args.methodology() == Methodology.CONVERGE) {
      times = new long[args.getWindow()];
    }
    verbose |= args.isDebug();
  }
  
  public void init(Config config) {
    if (verbose) System.out.println("Initializing callback");
    iterations = 0;
    
    switch (args.methodology()) {
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
      for (int i=0; i < times.length; i++)
        times[i] = 0;
  }
  
  /**
   * This method governs the benchmark iteration process.  The test
   * harness will run the benchmark repeatedly until this method
   * returns 'false'.
   * 
   * The default methodologies consist of 0 or more 'warmup' iterations,
   * followed by a single timing iteration.
   * 
   * @return Whether to run another iteration.
   */
  public boolean runAgain() {
    if (verbose) System.out.println("runAgain");
    /* Always quit immediately after the timing iteration */
    if (!isWarmup())
      return false;
    
    iterations++;
    if (verbose) System.out.println("iterations = "+iterations);
    switch (args.methodology()) {
    case ITERATE:
      if (iterations == args.getIterations() - 1)
        mode = Mode.TIMING;
      if (verbose) System.out.println("mode = "+mode);
      return true;
      
    case CONVERGE:
      /* If we've exceeded the maximum iterations, exit */
      if (iterations >= args.getMaxIterations()) {
        System.err.println("Benchmark failed to converge.");
        return false;
      }
      
      /* Maintain the sliding window of execution times */
      times[(iterations-1)%args.getWindow()] = elapsed;
      
      /* If we haven't filled the window, repeat immediately */
      if (iterations < args.getWindow())
        return true;
      
      /* Optionally report on progress towards convergence */
      if (iterations >= args.getWindow() && args.isVerbose()) {
        System.err.printf("Variation %4.2f%% achieved after %d iterations, target = %4.2f%%\n",
                TestHarness.coeff_of_var(times)*100,iterations,args.getTargetVar()*100);
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
    return false;  // Keep javac happy
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
  @Deprecated
  public void startWarmup(String benchmark) {
    start(benchmark, true);
  };
  protected void start(String benchmark, boolean warmup) {
    timer = System.currentTimeMillis();
    System.err.print("===== DaCapo " + TestHarness.getBuildVersion() + " " + benchmark + " starting ");
    System.err.println((warmup ? "warmup " : "") + "=====");
    System.err.flush();
  }
  
  /* Stop the timer */
  public void stop() {
    stop(mode == Mode.WARMUP);
  }
  @Deprecated
  public void stopWarmup() {
    stop(true);
  }
  public void stop(boolean warmup) {
    elapsed = System.currentTimeMillis() - timer;
  }
  
  /* Announce completion of the benchmark (pass or fail) */
  public void complete(String benchmark, boolean valid) {
    complete(benchmark, valid, mode == Mode.WARMUP);
  };
  @Deprecated
  public void completeWarmup(String benchmark, boolean valid) {
    complete(benchmark, valid, true);
  };
  protected void complete(String benchmark, boolean valid, boolean warmup) {
    System.err.print("===== DaCapo " + TestHarness.getBuildVersion() + " " + benchmark);
    if (valid) {
      System.err.print(warmup ? " completed warmup " : " PASSED ");
      System.err.print("in " + elapsed + " msec ");   
    } else {
      System.err.print(" FAILED " + (warmup ? "warmup " : ""));
    }
    System.err.println("=====");
    System.err.flush();
  }
  
}
