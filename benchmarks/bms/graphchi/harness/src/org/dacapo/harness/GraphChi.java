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
import java.util.Arrays;

import org.dacapo.harness.Benchmark;
import org.dacapo.parser.Config;

public class GraphChi extends Benchmark {

  private String[] args;
  private Class<?> cls;

  public GraphChi(Config config, File scratch, File data) throws Exception {
    super(config, scratch, data, false, false);
  }

  @Override
  protected void prepare(String size) throws Exception {
    super.prepare(size);

    String[] config_args = config.preprocessArgs(size, scratch, data);
    String strAppClsName = config_args[0];
    args = Arrays.copyOfRange(config_args, 1, config_args.length);
    
    cls = Class.forName("edu.cmu.graphchi.apps." + strAppClsName, true, loader);
    this.method = cls.getMethod("main", new Class[] { String[].class });
  }

  public void iterate(String size) throws Exception {
    method.invoke(null, (Object)args);
  }
}
