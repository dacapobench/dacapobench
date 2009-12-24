/*
 * Copyright (c) 2009 The Australian National University.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License v2.0.
 * You may obtain the license at
 * 
 *    http://www.opensource.org/licenses/apache2.0.php
 */
package org.dacapo.daytrader;

import java.io.InputStream;

import org.apache.geronimo.gbean.AbstractName;
import org.apache.geronimo.gbean.AbstractNameQuery;
import org.apache.geronimo.kernel.Jsr77Naming;
import org.apache.geronimo.kernel.Kernel;
import org.apache.geronimo.kernel.KernelFactory;
import org.apache.geronimo.kernel.config.ConfigurationUtil;
import org.apache.geronimo.kernel.config.ConfigurationManager;
import org.apache.geronimo.kernel.log.GeronimoLogging;
import org.apache.geronimo.kernel.repository.Artifact;

import java.util.Set;
import java.util.Collection;

/**
 * @date $Date: 2009-12-24 11:19:36 +1100 (Thu, 24 Dec 2009) $
 * @id $Id: DaCapoClientRunner.java 738 2009-12-24 00:19:36Z steveb-oss $
 */
public class DaCapoClientRunner {
  private static Kernel kernel = null;
  private static AbstractName bean = null;

  public static void initialize(String carName, String size, int numThreads, boolean useBeans) {
    try {
      GeronimoLogging.initialize(GeronimoLogging.ERROR);
      ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
      kernel = KernelFactory.newInstance().createKernel("DaCapoClient");
      kernel.boot();
      InputStream in = classLoader.getResourceAsStream("META-INF/config.ser");
      ConfigurationUtil.loadBootstrapConfiguration(kernel, in, classLoader, true);
      Artifact configuration = Artifact.create(carName);
      ConfigurationManager configurationManager = ConfigurationUtil.getConfigurationManager(kernel);
      configurationManager.loadConfiguration(configuration);
      configurationManager.startConfiguration(configuration);
      ConfigurationUtil.releaseConfigurationManager(kernel, configurationManager);
      bean = new Jsr77Naming().createRootName(configuration, configuration.toString(), "J2EEApplication");
      String[] args = new String[] { "-i", "-t", numThreads + "", "-s", size, useBeans ? "-b" : "" };
      kernel.invoke(bean, "main", new Object[] { args }, new String[] { String[].class.getName() });
    } catch (Exception e) {
      System.err.print("Exception initializing client: " + e.toString());
      e.printStackTrace();
      System.exit(-1);
    }
  }

  public static void runIteration(String size, int numThreads, boolean useBeans) {
    String[] args = new String[] { "-t", numThreads + "", "-s", size, useBeans ? "-b" : "" };
    try {
      kernel.invoke(bean, "main", new Object[] { args }, new String[] { String[].class.getName() });
    } catch (Exception e) {
      System.err.print("Exception running client iteration: " + e.toString());
      e.printStackTrace();
    }
  }

  public static void shutdown() {
    bean = null;
    kernel.shutdown();
    kernel = null;
  }
}
