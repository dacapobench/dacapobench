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

//import org.eclipse.core.runtime.adaptor.EclipseStarter;
import java.lang.reflect.Method;
import org.dacapo.harness.Benchmark;
import org.dacapo.parser.Config;

/**
 * @date $Date: 2009-12-24 11:19:36 +1100 (Thu, 24 Dec 2009) $
 * @id $Id: Eclipse.java 738 2009-12-24 00:19:36Z steveb-oss $
 */
public class Eclipse extends Benchmark {

  static final String WKSP_DIRECTORY_RELATIVE_TO_SCRATCH = "workspace";
  static final String PLUGIN_ID = "org.dacapo.eclipse.Harness";
  static final String OSGI_BOOTSTRAP_JAR = "eclipse" + File.separator + "plugins" + File.separator + "org.eclipse.osgi_3.5.1.R35x_v20090827.jar";

  static String oldJavaHome = null;
  static Eclipse eclipse;
  static String size;

  private final Method isRunning;
  private final Method run;
  private final Method shutdown;

  public Eclipse(Config config, File scratch) throws Exception {
    super(config, scratch, false);
    Class<?> clazz = Class.forName("org.eclipse.core.runtime.adaptor.EclipseStarter", true, loader);
    this.method = clazz.getMethod("startup", String[].class, Runnable.class);
    this.isRunning = clazz.getMethod("isRunning");
    this.run = clazz.getMethod("run", Object.class);
    this.shutdown = clazz.getMethod("shutdown");
  }

  public void preIteration(String size) throws Exception {
    super.preIteration(size);
    if (!((Boolean) isRunning.invoke(null, (Object[]) null)).booleanValue()) {
      startup(size);
    }
    setJavaHomeIfRequired();
    try {
      String[] largePluginArgs = { "large", "unzip" };
      String[] pluginArgs = { "unzip" };
      run.invoke(null, new Object[] { size.equals("large") ? largePluginArgs : pluginArgs });
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void iterate(String size) throws Exception {
    try {
      // String[] pluginArgs = {"setup", "alltests" }; // get this from the bm
      // config
      String[] pluginArgs = config.getArgs(size);
      run.invoke(null, new Object[] { pluginArgs });
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void postIteration(String size) throws Exception {
    try {
      String[] pluginArgs = { "teardown" };
      run.invoke(null, new Object[] { pluginArgs });
    } catch (Exception e) {
      e.printStackTrace();
    }
    super.postIteration(size);
    restoreJavaHomeIfRequired();
  }

  public void cleanup() {
    try {
      shutdown.invoke(null, (Object[]) null);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void startup(String size) {
    try {
      System.setProperty("osgi.os", "linux");
      System.setProperty("osgi.ws", "gtk");
      System.setProperty("osgi.arch", "x86");
      System.setProperty("osgi.install.area", "file:" + fileInScratch("eclipse/"));
      System.setProperty("osgi.noShutdown", "true");
      System.setProperty("osgi.framework", "file:" + fileInScratch(OSGI_BOOTSTRAP_JAR));
      setJavaHomeIfRequired();

      String[] args = new String[4];
      args[0] = "-data"; // identify the workspace
      args[1] = fileInScratch(WKSP_DIRECTORY_RELATIVE_TO_SCRATCH);
      args[2] = "-application"; // identify the plugin
      args[3] = PLUGIN_ID;
      method.invoke(null, new Object[] { args, null });
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void setJavaHomeIfRequired() {
    String eclipseJavaHome = System.getProperty("eclipse.java.home");
    if (eclipseJavaHome != null) {
      oldJavaHome = System.getProperty("java.home");
      System.setProperty("java.home", eclipseJavaHome);
    }
  }

  private void restoreJavaHomeIfRequired() {
    if (oldJavaHome != null)
      System.setProperty("java.home", oldJavaHome);
  }
}
