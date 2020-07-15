/*
 * Copyright (c) 2018 The Australian National University.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License v2.0.
 * You may obtain the license at
 *
 *    http://www.opensource.org/licenses/apache2.0.php
 */

package org.dacapo.harness;

import org.dacapo.parser.Config;

import java.io.File;
import java.util.Arrays;

/**
 * Dacapo benchmark harness for zxing
 *
 * date:  $Date: 2018-12-4 20:57:36 +1100 (Tue, 4 Dec 2018) $
 * id: $Id: Zxing.java 738 2018-12-04 20:57:36 steveb-oss $
 */
public class Zxing extends Benchmark{

  String[] args;

  public Zxing(Config config, File scratch, File data) throws Exception {
      super(config, scratch, data, false, false);
      Class<?> clazz = Class.forName("com.google.zxing.client.j2se.CommandLineRunner", true, loader);
      this.method = clazz.getMethod("main", String[].class);
  }

  @Override
  protected void prepare(String size) throws Exception {
    super.prepare(size);
    args = config.preprocessArgs(size, scratch, data);
  }

  @Override
  public void iterate(String size){
    try {
      String barcode = args[0];
      // Complete the path of each barcode
      this.method.invoke(null, (Object) new String[] {"--recursive", "--brief", "--try_harder", barcode});
    } catch (Exception e){
      e.printStackTrace();
    }
  }
}
