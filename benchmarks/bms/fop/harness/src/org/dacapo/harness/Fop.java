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
import java.util.Properties;

import org.dacapo.harness.Benchmark;
import org.dacapo.parser.Config;

/**
 * @date $Date: 2009-12-24 11:19:36 +1100 (Thu, 24 Dec 2009) $
 * @id $Id: Fop.java 738 2009-12-24 00:19:36Z steveb-oss $
 */
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

  public void iterate(String size) throws Exception {
    method.invoke(null, new Object[] { args });
  }
}
