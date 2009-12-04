/*******************************************************************************
 * Copyright (c) 2006, 2009 The Australian National University.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License v2.0
 *
 * @date $Date: 2009-12-04 14:33:59 +1100 (Fri, 04 Dec 2009) $
 * @id $Id: MyCallback.java 659 2009-12-04 03:33:59Z jzigman $
 *******************************************************************************/
import org.dacapo.harness.Callback;
import org.dacapo.harness.CommandLineArgs;

public class MyCallback extends Callback {

  public MyCallback(CommandLineArgs args) {
    super(args);
  }

  /* Immediately prior to start of the benchmark */
  public void start(String benchmark) {
    System.err.println("my hook starting " + (isWarmup() ? "warmup " : "")
        + benchmark);
    super.start(benchmark);
  };

  /* Immediately after the end of the benchmark */
  public void stop() {
    super.stop();
    System.err.println("my hook stopped " + (isWarmup() ? "warmup" : ""));
    System.err.flush();
  };

  public void complete(String benchmark, boolean valid) {
    super.complete(benchmark, valid);
    System.err.println("my hook " + (valid ? "PASSED " : "FAILED ")
        + (isWarmup() ? "warmup " : "") + benchmark);
    System.err.flush();
  };
}
