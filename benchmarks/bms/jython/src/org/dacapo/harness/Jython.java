/*
 * Copyright (c) 2005, 2009 The Australian National University.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License v2.0.
 * You may obtain the license at
 * 
 *    http://www.opensource.org/licenses/apache2.0.php
 */
package org.dacapo.harness;

import java.io.File;
import java.lang.reflect.Method;

import org.dacapo.harness.Benchmark;
import org.dacapo.parser.Config;

/**
 * @date $Date: 2009-12-24 11:19:36 +1100 (Thu, 24 Dec 2009) $
 * @id $Id: Jython.java 738 2009-12-24 00:19:36Z steveb-oss $
 */
public class Jython extends Benchmark {

  private Method pySetArgsMethod;

  public Jython(Config config, File scratch) throws Exception {
    super(config, scratch);
    Class<?> clazz = Class.forName("org.python.util.jython", true, loader);
    this.method = clazz.getMethod("main", String[].class);
    Class<?> pyClass = Class.forName("org.python.core.PySystemState", true, loader);
    pySetArgsMethod = pyClass.getMethod("setArgv", String[].class);
    System.setProperty("python.home", fileInScratch("jython"));
    System.setProperty("python.cachedir", fileInScratch("cachedir"));
    System.setProperty("python.verbose", "warning");
    useBenchmarkClassLoader();
    try {
      method.invoke(null, (Object) new String[] { fileInScratch("jython/noop.py") });
    } finally {
      revertClassLoader();
    }
  }

  /**
   * jython.main doesn't expect to be called multiple times, so we've hacked
   * Py.setArgv to allow us to reset the command line arguments that the python
   * script sees. Hence the Py.setArgv call, followed by the jython.main call.
   */
  public void iterate(String size) throws Exception {
    String[] args = config.preprocessArgs(size, scratch);
    pySetArgsMethod.invoke(null, (Object) args);
    method.invoke(null, (Object) args);
  }

  public void cleanup() {
    super.cleanup();
    deleteTree(new File(scratch, "cachedir"));
  }
}
