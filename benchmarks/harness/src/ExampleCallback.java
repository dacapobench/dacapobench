/*
 * Copyright (c) 2006, 2009 The Australian National University.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License v2.0.
 * You may obtain the license at
 * 
 *    http://www.opensource.org/licenses/apache2.0.php
 */
import org.dacapo.harness.Callback;
import org.dacapo.harness.CommandLineArgs;

public class ExampleCallback extends Callback {

  public ExampleCallback(CommandLineArgs args) {
    super(args);
  }

  /* Immediately prior to start of the benchmark */
  @Override
  public void start(String benchmark) {
    System.err.println("Example callback starting " + (isWarmup() ? "warmup " : "") + benchmark);
    super.start(benchmark);
  };

  /* Immediately after the end of the benchmark */
  @Override
  public void stop(long duration) {
    super.stop(duration);
    System.err.println("Example callback stopping " + (isWarmup() ? "warmup" : ""));
    System.err.flush();
  };

  @Override
  public void complete(String benchmark, boolean valid) {
    super.complete(benchmark, valid);
    System.err.println("Example callback " + (valid ? "PASSED " : "FAILED ") + (isWarmup() ? "warmup " : "") + benchmark + (requests > 0 ? (", observed "+requests+" requests") : ""));
    System.err.flush();
  };

  /**
   * The workload is about to start issuing requests
   */
  public void requestsStarting() { /* your code here */}

    /**
   * The workload has finished issuing requests.
   */
  public void requestsFinished() { /* your code here */}


  /* let's count the number of requests we observe */
  private int requests = 0;
  synchronized
  private void inc() {
    requests++;
  }

  /* Called immediately before a request begins */
  @Override
  public void requestStart(int id) { inc(); /* your code here */}

  /* Called immediately after a request completes */
  @Override
  public void requestEnd(int id) { /* your code here */}
}
