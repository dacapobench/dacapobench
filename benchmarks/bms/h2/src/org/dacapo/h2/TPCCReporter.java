/*
 * Copyright (c) 2009 The Australian National University.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License v2.0.
 * You may obtain the license at
 * 
 *    http://www.opensource.org/licenses/apache2.0.php
 */
package org.dacapo.h2;

import java.io.FileWriter;

/**
 * date:  $Date: 2009-12-24 11:19:36 +1100 (Thu, 24 Dec 2009) $
 * id: $Id: TPCCReporter.java 738 2009-12-24 00:19:36Z steveb-oss $
 */
public class TPCCReporter {
  private int count = 0;
  private int totalTransactions = 0;
   
  public TPCCReporter() {
  }

  public synchronized void reset(int totalTransactions) {
    this.totalTransactions = totalTransactions;
    count = 0;
  }

  public synchronized void done() {
    count++;
    int fivePercent = totalTransactions / 20;
    if (count % fivePercent == 0) {
      System.out.print("\r"+(5*count / fivePercent)+"%");
    }
  }
}
