/*******************************************************************************
 * Copyright (c) 2005, 2009 The Australian National University.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License v2.0
 *
 * @date $Date: 2009-12-21 14:50:19 +1100 (Mon, 21 Dec 2009) $
 * @id $Id: Fop.java 721 2009-12-21 03:50:19Z steveb-oss $
 *******************************************************************************/
package org.dacapo.harness;

import java.io.File;

import org.dacapo.harness.Benchmark;
import org.dacapo.parser.Config;

public class Fop extends Benchmark {

  private String[] args;

  public Fop(Config config, File scratch) throws Exception {
    super(config, scratch);
    Class<?> clazz = Class.forName("org.apache.fop.cli.Main", true, loader);
    this.method = clazz.getMethod("startFOP", new Class[] { String[].class });
  }

  @Override
  protected void prepare(String size) throws Exception {
    super.prepare(size);
    args = config.preprocessArgs(size, scratch);
    /* Retarget input/output files into scratch directory */
    for (int i = 0; i < args.length; i++)
      if (args[i].charAt(0) != '-')
        args[i] = fileInScratch(args[i]);
  }

  public void iterate(String size) throws Exception {
    method.invoke(null, new Object[] { args });
  }
}
