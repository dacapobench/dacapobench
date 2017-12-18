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
import java.util.Iterator;
import java.util.List;

import org.dacapo.harness.Benchmark;
import org.dacapo.parser.Config;

/**
 * @date $Date: 2009-12-24 11:19:36 +1100 (Thu, 24 Dec 2009) $
 * @id $Id: Pmd.java 738 2009-12-24 00:19:36Z steveb-oss $
 */
public class Pmd extends Benchmark {

  String[] args;

  public Pmd(Config config, File scratch) throws Exception {
    super(config, scratch);
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

  /**
   * Generate a new file list that has the scrath directory path
   * prepended to each file in the list
   */
  private File prepended_filelist(String filelist_path) {
    try {
      File fl = new File(filelist_path);
      BufferedReader reader = new BufferedReader(new FileReader(fl));
      List<String> lst = new ArrayList<String>();
      for (String l = reader.readLine(); l != null; l = reader.readLine())
        lst.add(fileInScratch(l));
      reader.close();


      fl.renameTo(new File(fl.getParentFile(), fl.getName() + ".orig"));

      File newfl = new File(filelist_path);
      newfl.createNewFile();
      BufferedWriter writer = new BufferedWriter(new FileWriter(newfl));

      for (Iterator<String> iter = lst.iterator(); iter.hasNext();)
        writer.write(iter.next() + "\n");
      writer.close();

      return newfl;

    } catch (FileNotFoundException e) {
      throw new RuntimeException("File " + filelist_path + " error: " + e);
    } catch (IOException e) {
      throw new RuntimeException("File " + filelist_path + " error: " + e);
    }
  }

  public void prepare(String size) {
      String [] config_args = config.getArgs(size);
      args = new String[12];

      args[0] = "-filelist";
      args[1] = prepended_filelist(fileInScratch(config_args[0].substring(1))).getPath();

      args[2] = "-format";
      args[3] = "text";

      // Java 1.6
      args[4] = "-language";
      args[5] = "java";
      args[6] = "-version";
      args[7] = "1.6";

      // if this is set to true, PMD will exit with status 4 on finding rule violations.
      // however we don't care about rule violations for benchmarking
      args[8] = "-failOnViolation";
      args[9] = "false";

      args[10] = "-rulesets";
      List<String> rulesets = new ArrayList<String>(args.length - 2);
      for (int i = 2; i < config_args.length; i ++) {
        if (config_args[i].charAt(0) != '-')
          rulesets.add(fileInScratch(config_args[i]));
      }
      args[11] = String.join(",", rulesets);
  }

  public void iterate(String size) throws Exception {
    method.invoke(null, (Object) args);
  }
}
