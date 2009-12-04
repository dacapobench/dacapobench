/*******************************************************************************
 * Copyright (c) 2006, 2009 The Australian National University.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License v2.0
 *
 * @date $Date: 2009-12-04 14:33:59 +1100 (Fri, 04 Dec 2009) $
 * @id $Id: GcCallback.java 659 2009-12-04 03:33:59Z jzigman $
 *******************************************************************************/
import org.dacapo.harness.Callback;
import org.dacapo.harness.CommandLineArgs;

public class GcCallback extends Callback {

  public GcCallback(CommandLineArgs args) {
    super(args);
  }

  /* perform a System.gc() before starting each run */
  public void start(String benchmark) {
    System.gc();
    super.start(benchmark);
  };

  public void startWarmup(String benchmark) {
    System.gc();
    super.start(benchmark);
  };
}
