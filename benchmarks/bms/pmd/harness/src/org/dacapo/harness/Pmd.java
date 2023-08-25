/*
 * Copyright (c) 2005, 2009 The Australian National University.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License v2.0.
 * You may obtain the license at
 * 
 *    http://www.opensource.org/licenses/apache2.0.php
 */
package org.dacapo.harness;

import java.io.BufferedWriter;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.dacapo.harness.Benchmark;
import org.dacapo.parser.Config;

/**
 * date:  $Date: 2009-12-24 11:19:36 +1100 (Thu, 24 Dec 2009) $
 * id: $Id: Pmd.java 738 2009-12-24 00:19:36Z steveb-oss $
 */
public class Pmd extends Benchmark {

  String[] args;
  String reportFileName = null;

  public Pmd(Config config, File scratch, File data) throws Exception {
    super(config, scratch, data);
    Class<?> clazz = Class.forName("net.sourceforge.pmd.PMD", true, loader);
    this.method = clazz.getMethod("main", String[].class);
    
    /*
     * Explicitly set some properties that control factory methods
     * 
     * Leaving it to the standard methods of resolving at runtime can lead to
     * testing different implementations on different platforms.
     * 
     * It's always possible that there are additional properties that need to be
     * set.
     */
    System.setProperty("javax.xml.parsers.DocumentBuilderFactory", "org.apache.xerces.jaxp.DocumentBuilderFactoryImpl");

    // Set System property so that PMD won't call System.exit() after run.
    Class pmdcli = Class.forName("net.sourceforge.pmd.cli.PMDCommandLineInterface", true, loader);
    System.setProperty((String)pmdcli.getField("NO_EXIT_AFTER_RUN").get(null), "true");
  }

  private void sortReport() {
    ArrayList<String> report = new ArrayList<String>();

    try {
      BufferedReader in = new BufferedReader(new FileReader(reportFileName));    
      String next = in.readLine();
      while (next != null) {
        report.add(next);
        next = in.readLine();
      }
      in.close();

      Collections.sort(report);

      BufferedWriter out = new BufferedWriter(new FileWriter(reportFileName));
      for (String line : report) {
        out.write(line);
        out.newLine();
      }
      out.close();
  } catch (IOException e) {
    System.err.println("Failed trying to sort pmd report file '"+reportFileName+"' "+e);
  }
  }

  @Override
  public boolean validate(String size) {
    sortReport();
    return super.validate(size);
  }

  protected void prepare(String size) throws Exception {
    super.prepare(size);
    args = config.preprocessArgs(size, scratch, data);
    for (int i = 0; i < args.length; i++) {
      if (args[i].equals("-r"))
        reportFileName = args[i+1];
    }
  }

  public void iterate(String size) throws Exception {
    method.invoke(null, (Object) args);
  }
}
