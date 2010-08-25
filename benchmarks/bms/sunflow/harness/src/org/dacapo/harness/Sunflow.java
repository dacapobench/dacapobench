/*
 * Copyright (c) 2009 The Australian National University.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License v2.0.
 * You may obtain the license at
 * 
 *    http://www.opensource.org/licenses/apache2.0.php
 */
package org.dacapo.harness;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import org.dacapo.parser.Config;

/**
 * @date $Date: 2009-12-24 11:19:36 +1100 (Thu, 24 Dec 2009) $
 * @id $Id: Sunflow.java 738 2009-12-24 00:19:36Z steveb-oss $
 */
public class Sunflow extends org.dacapo.harness.Benchmark {

  private final Constructor<?> constructor;
  private Object sunflow;
  private final Method beginMethod;
  private final Method endMethod;

  public Sunflow(Config config, File scratch) throws Exception {
    super(config, scratch, false);
    Class<?> clazz = Class.forName("org.sunflow.Benchmark", true, loader);
    this.method = clazz.getMethod("kernelMain");
    this.beginMethod = clazz.getMethod("kernelBegin");
    this.endMethod = clazz.getMethod("kernelEnd");
    this.constructor = clazz.getConstructor(int.class, boolean.class, boolean.class, boolean.class, int.class);
  }

  /** Do one-time prep such as unziping data. In our case, do nothing. */
  protected void prepare() {
  }

  /**
   * Code to execute prior to each iteration OUTSIDE the timing loop. In this
   * case we create a new instance of a Sunflow benchmark, which sets up basic
   * data structures.
   * 
   * @param size The "size" of the iteration (small, default, large)
   */
  public void preIteration(String size) throws Exception {
    String[] args = config.preprocessArgs(size, scratch);
    useBenchmarkClassLoader();
    try {
      sunflow = constructor.newInstance(Integer.parseInt(args[0]), false, false, false, config.getThreadCount(size));
      beginMethod.invoke(sunflow);
    } finally {
      revertClassLoader();
    }
  }

  /**
   * Perform a single iteration of the benchmark.
   * 
   * @param size The "size" of the iteration (small, default, large)
   */
  public void iterate(String size) throws Exception {
    method.invoke(sunflow);
  }

  /**
   * Validate the output of the benchmark, OUTSIDE the timing loop.
   * 
   * @param size The "size" of the iteration (small, default, large)
   */
  public boolean validate(String size) {
    if (!getValidate())
      return true;
    try {
      useBenchmarkClassLoader();
      try {
        endMethod.invoke(sunflow);
      } finally {
        revertClassLoader();
      }
    } catch (RuntimeException e) {
      System.err.println(e.getMessage());
      return false;
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
    return super.validate(size);
  }
}
