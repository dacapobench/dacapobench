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

import org.dacapo.harness.Benchmark;
import org.dacapo.harness.DacapoException;
import org.dacapo.parser.Config;

/**
 * @date $Date: 2009-12-24 11:19:36 +1100 (Thu, 24 Dec 2009) $
 * @id $Id: Luindex.java 738 2009-12-24 00:19:36Z steveb-oss $
 */
public class Luindex extends Benchmark {

  private final Object benchmark;

  public Luindex(Config config, File scratch) throws Exception {
    super(config, scratch);
    Class<?> clazz = Class.forName("org.dacapo.luindex.Index", true, loader);
    this.method = clazz.getMethod("main", File.class, String[].class);
    Constructor<?> cons = clazz.getConstructor(File.class);
    useBenchmarkClassLoader();
    try {
      benchmark = cons.newInstance(scratch);
    } finally {
      revertClassLoader();
    }
  }

  public void cleanup() {
    if (!getPreserve()) {
      deleteTree(new File(scratch, "luindex"));
    }
  }

  public void preIteration(String size) {
    if (getPreserve() && getIteration() > 1) {
      deleteTree(new File(scratch, "index"));
    }
  }

  /**
   * Index all text files under a directory.
   */
  public void iterate(String size) throws Exception {
    if (getVerbose())
      System.out.println("luindex benchmark starting");
    String[] args = config.preprocessArgs(size, scratch);

    final File INDEX_DIR = new File(scratch, "index");

    if (INDEX_DIR.exists()) {
      System.out.println("Cannot save index to '" + INDEX_DIR + "' directory, please delete it first");
      throw new DacapoException("Cannot write to index directory");
    }

    method.invoke(benchmark, INDEX_DIR, args);
  }

  public void postIteration(String size) {
    if (!getPreserve()) {
      deleteTree(new File(scratch, "index"));
    }
  }
}
