/*******************************************************************************
 * Copyright (c) 2009 The Australian National University.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License v2.0
 *
 * @date $Date: 2009-12-24 11:19:36 +1100 (Thu, 24 Dec 2009) $
 * @id $Id: H2.java 738 2009-12-24 00:19:36Z steveb-oss $
 *******************************************************************************/
package org.dacapo.harness;

import java.io.File;
import java.io.PrintStream;
import java.lang.reflect.Method;

import org.dacapo.harness.Benchmark;
import org.dacapo.parser.Config;

/**
 * Dacapo benchmark harness for TPC-C like workload running on H2.
 * 
 * Apache authored the original TPC-C like workload.
 * H2 Group, H2 authored the database H2.
 * 
 * @author Apache
 * @author H2
 *
 */
public class H2 extends Benchmark {

  private Object tpcc;
  private Method makeTPCC;
  private Method prepareTPCC;
  private Method preIterationTPCC;
  private Method iterationTPCC;
  private Method postIterationTPCC;
  
  public H2(Config config, File scratch) throws Exception {
    super(config, scratch, false);
  }
  
  @Override
  protected void prepare() throws Exception {
    useBenchmarkClassLoader();
    try {
      Class<?> tpccClazz  = Class.forName("org.dacapo.h2.TPCC",true,loader);
      this.makeTPCC = tpccClazz.getMethod("make", Config.class, File.class, Boolean.class, Boolean.class);
      this.prepareTPCC = tpccClazz.getMethod("prepare", String.class);
      this.preIterationTPCC = tpccClazz.getMethod("preIteration", String.class);
      this.iterationTPCC = tpccClazz.getMethod("iteration", String.class);
      this.postIterationTPCC = tpccClazz.getMethod("postIteration", String.class);

      // construct the benchmark
      this.tpcc = this.makeTPCC.invoke(null, config, scratch, getVerbose(), getPreserve());
    } finally {
      revertClassLoader();
    }
  }

  
  /**
   * The benchmark run 
   */
  @Override
  public void prepare(String size) throws Exception {
    useBenchmarkClassLoader();
    try {
      this.prepareTPCC.invoke(this.tpcc, size);
    } finally {
      revertClassLoader();
    }
  }

  @Override
  public void preIteration(String size) throws Exception {
    useBenchmarkClassLoader();
    try {
      this.preIterationTPCC.invoke(this.tpcc, size);
    } finally {
      revertClassLoader();
    }
  }
  
  @Override
  public void iterate(String size) throws Exception {
    useBenchmarkClassLoader();
    try {
      this.iterationTPCC.invoke(this.tpcc, size);
    } finally {
      revertClassLoader();
    }
  }
  
  @Override
  public void postIteration(String size) throws Exception {
    useBenchmarkClassLoader();
    try {
      this.postIterationTPCC.invoke(this.tpcc, size);
    } finally {
      revertClassLoader();
      super.postIteration(size);
    }
  }
  
  @Override
  public void cleanup() {
	this.tpcc = null;
	this.makeTPCC = null;
	this.prepareTPCC = null;
	this.preIterationTPCC = null;
	this.iterationTPCC = null;
	this.postIterationTPCC = null;
	
	super.cleanup();
  }
}
