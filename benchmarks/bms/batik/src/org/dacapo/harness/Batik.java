/*******************************************************************************
 * Copyright (c) 2005, 2009 The Australian National University.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License v2.0
 *
 * @date $Date: 2009-12-21 14:50:19 +1100 (Mon, 21 Dec 2009) $
 * @id $Id: Batik.java 721 2009-12-21 03:50:19Z steveb-oss $
 *******************************************************************************/
package org.dacapo.harness;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.Vector;

import org.dacapo.harness.Benchmark;
import org.dacapo.parser.Config;

public class Batik extends Benchmark {

  private String[] args;
  private final Constructor<?> constructor;

  public Batik(Config config, File scratch) throws Exception {
    super(config, scratch, false);
    Class<?> clazz = Class.forName("org.apache.batik.apps.rasterizer.Main",
        true, loader);
    this.method = clazz.getMethod("execute");
    this.constructor = clazz.getConstructor(String[].class);
  }

  @Override
  protected void prepare(String size) throws Exception {
    super.prepare(size);
    String[] args = config.preprocessArgs(size, scratch);
    Vector<String> newArgs = new Vector<String>(args.length + 2);
    for (int i = 0; i < args.length; i++) {
      if (args[i].charAt(0) == '-') {
        if (args[i].equals("-m") || args[i].equals("-d")) {
          newArgs.add(args[i]);
          newArgs.add(args[++i]);
        } else
          newArgs.add(args[i]);
      } else
        newArgs.add((new File(scratch, args[i])).getPath());
    }
    String[] newArgStrings = (String[]) newArgs.toArray(new String[0]);
    if (getVerbose()) {
      for (int i = 0; i < newArgStrings.length; i++)
        System.out.print(newArgStrings[i] + " ");
      System.out.println();
    }
    this.args = newArgStrings;
  }

  /**
   * Args is a list of file names relative to the scratch directory
   */
  public void iterate(String size) throws Exception {
    Object object = constructor.newInstance((Object) args);
    method.invoke(object);
  }
}
