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

import org.dacapo.harness.Benchmark;
import org.dacapo.parser.Config;

/**
 * @date $Date: 2009-12-24 11:19:36 +1100 (Thu, 24 Dec 2009) $
 * @id $Id: Avrora.java 738 2009-12-24 00:19:36Z steveb-oss $
 */
public class Avrora extends Benchmark {

  String[] args;

  public Avrora(Config config, File scratch) throws Exception {
    super(config, scratch);
    Class<?> clazz = Class.forName("avrora.Main", true, loader);
    this.method = clazz.getMethod("main", String[].class);
  }

  @Override
  protected void prepare(String size) throws Exception {
    super.prepare(size);
    args = config.preprocessArgs(size, scratch);
    /* Retarget input/output files into scratch directory */
    for (int i = 0; i < args.length; i++) {
      if (args[i].charAt(0) != '-')
        args[i] = fileInScratch(args[i]);
    }
  }

  public void iterate(String size) throws Exception {
    method.invoke(null, (Object) args);
  }
}
