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

/**
 * @date $Date: 2009-12-24 11:19:36 +1100 (Thu, 24 Dec 2009) $
 * @id $Id: MyCallback.java 738 2009-12-24 00:19:36Z steveb-oss $
 */
public class MyCallback extends Callback {

  public MyCallback(CommandLineArgs args) {
    super(args);
  }

  /* Immediately prior to start of the benchmark */
  public void start(String benchmark) {
    System.err.println("my hook starting " + (isWarmup() ? "warmup " : "") + benchmark);
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
    System.err.println("my hook " + (valid ? "PASSED " : "FAILED ") + (isWarmup() ? "warmup " : "") + benchmark);
    System.err.flush();
  };
}
