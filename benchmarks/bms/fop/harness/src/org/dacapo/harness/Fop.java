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
import java.util.Arrays;
import java.util.Properties;

import org.dacapo.harness.Benchmark;
import org.dacapo.parser.Config;

/**
 * date:  $Date: 2009-12-24 11:19:36 +1100 (Thu, 24 Dec 2009) $
 * id: $Id: Fop.java 738 2009-12-24 00:19:36Z steveb-oss $
 */
public class Fop extends Benchmark {

  private String[] args;
  private String[] names;
  private String[] inputs;
  private String[] outputs;

  public Fop(Config config, File scratch, File data) throws Exception {
    super(config, scratch, data, false, false);
    Class<?> clazz = Class.forName("org.apache.fop.cli.Main", true, loader);
    this.method = clazz.getMethod("startFOP", new Class[] { String[].class });
  }

  @Override
  protected void prepare(String size) throws Exception {
    super.prepare(size);
    args = config.preprocessArgs(size, scratch, data);
    collectInputs();
  }

  private void collectInputs() {
    File src = new File(args[0]);
    File dst = new File(args[1]);
    String [] list = src.list();
    String srcdir = src.getPath();
    if (list == null) { // single file
      list = new String[1];
      list[0] = src.getName();
      srcdir = src.getParent();
    } else {
      Arrays.sort(list);
    }
    inputs = new String [list.length];
    outputs = new String [list.length];
    names = new String [list.length];
    String targetFormat = args[2].substring(1);
    for (int i = 0; i < list.length; i ++) {
      names[i] = list[i];
      inputs[i] = srcdir + File.separatorChar + list[i];
      outputs[i] = dst.getPath() + File.separatorChar + 
                    list[i].substring(0, list[i].lastIndexOf(".fo") + 1) + targetFormat;
    }
  }

  @Override
  public void augmentSystemProperties(Properties systemProperties) {

    /*
     * The benchmark attempts to access an external font cache under
     * "${user.home}/.fop/fop-fonts.cache". Make sure that no such cache is
     * found.
     */
    systemProperties.setProperty("user.home", fileInScratch(config.name));

    /*
     * Clear all logging-related system properties (except for
     * "java.util.logging.config.file") to make sure that the default logging
     * implementation is used.
     */
    systemProperties.remove("org.apache.commons.logging.LogFactory");
    systemProperties.remove("org.apache.commons.logging.Log");
    systemProperties.remove("org.apache.commons.logging.log");
    systemProperties.remove("java.util.logging.manager");
    systemProperties.remove("java.util.logging.config.class");

    /*
     * Make sure that JAXP debug messages are disabled.
     */
    systemProperties.remove("jaxp.debug");
  }

  public void iterate(String size) {
    try {
      String [] invokeArgs = new String[4];
      invokeArgs[0] = "-q";
      invokeArgs[2] = args[2];
      for (int i = 0; i < inputs.length; i ++) {
        invokeArgs[1] = inputs[i];
        invokeArgs[3] = outputs[i];
        System.out.print(names[i]+" ");
        method.invoke(null, new Object[] { invokeArgs });
      }
      System.out.println();
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(-1);
    }
  }
}
