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
import java.lang.reflect.Constructor;

import org.dacapo.parser.Config;

/**
 * @date $Date: 2009-12-24 11:19:36 +1100 (Thu, 24 Dec 2009) $
 * @id $Id: LusearchFix.java 738 2009-12-24 00:19:36Z steveb-oss $
 */
public class LusearchFix extends org.dacapo.harness.Benchmark {
  private final Object benchmark;

  public LusearchFix(Config config, File scratch) throws Exception {
    super(config, scratch, false);
    Class<?> clazz = Class.forName("org.dacapo.lusearchFix.Search", true, loader);
    this.method = clazz.getMethod("main", String[].class);
    Constructor<?> cons = clazz.getConstructor();
    useBenchmarkClassLoader();
    try {
      benchmark = cons.newInstance();
    } finally {
      revertClassLoader();
    }
  }

  @Override
  public void iterate(String size) throws Exception {
    method.invoke(benchmark, (Object) (config.preprocessArgs(size, scratch)));
  }
}
