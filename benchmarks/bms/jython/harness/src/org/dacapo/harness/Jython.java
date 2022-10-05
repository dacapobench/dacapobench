/*
 * Copyright (c) 2005, 2009 The Australian National University.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License v2.0.
 * You may obtain the license at
 * 
 *    http://www.opensource.org/licenses/apache2.0.php
 */
package org.dacapo.harness;

import java.io.*;
import java.lang.reflect.Method;
import java.nio.file.*;

import org.dacapo.harness.Benchmark;
import org.dacapo.parser.Config;

/**
 * date:  $Date: 2009-12-24 11:19:36 +1100 (Thu, 24 Dec 2009) $
 * id: $Id: Jython.java 738 2009-12-24 00:19:36Z steveb-oss $
 */
public class Jython extends Benchmark {

  private Method pySetArgsMethod;

  public Jython(Config config, File scratch, File data) throws Exception {
    super(config, scratch, data, false, false);
    Class<?> clazz = Class.forName("org.python.util.jython", true, loader);
    this.method = clazz.getMethod("main", String[].class);
    Class<?> pyClass = Class.forName("org.python.core.PySystemState", true, loader);
    pySetArgsMethod = pyClass.getMethod("setArgv", String[].class);
    System.setProperty("python.home", fileInData("dat"+File.separator+"jython"));
    System.setProperty("python.cachedir", fileInScratch("cachedir"));
    System.setProperty("python.verbose", "warning");
    System.setProperty("python.console", "org.python.core.PlainConsole");
    useBenchmarkClassLoader();
    Path dat = Paths.get(fileInData("dat"+File.separator+"jython"+File.separator+"noop.py"));
    Path tmp = Paths.get(scratch.getAbsolutePath()+File.separator+"noop.py");
    Files.copy(dat, tmp);
    try {
       method.invoke(null, (Object) new String[] { scratch.getAbsolutePath()+File.separator+"noop.py" });
     } finally {
       revertClassLoader();
     }
  }

  @Override
  public void preIteration(String size) throws Exception {
    super.preIteration(size);
    Path dat = Paths.get(fileInData("dat"+File.separator+"jython"+File.separator+"pybench"));
    Path tmp = Paths.get(scratch.getAbsolutePath()+File.separator+"pybench");
    Files.walkFileTree(dat, new FileCopy(dat, tmp));
  }

  /**
   * jython.main doesn't expect to be called multiple times, so we've hacked
   * Py.setArgv to allow us to reset the command line arguments that the python
   * script sees. Hence the Py.setArgv call, followed by the jython.main call.
   */
  public void iterate(String size) throws Exception {
    String[] args = config.preprocessArgs(size, scratch, data);
    pySetArgsMethod.invoke(null, (Object) args);
    method.invoke(null, (Object) args);
  }

  @Override
  public void postIteration(String size) throws Exception {
    super.postIteration(size);
    deleteTree(new File(scratch, "pybench"));
    deleteTree(new File(scratch, "noop.py"));
  }

  public void cleanup() {
    super.cleanup();
    deleteTree(new File(scratch, "cachedir"));
  }
}
