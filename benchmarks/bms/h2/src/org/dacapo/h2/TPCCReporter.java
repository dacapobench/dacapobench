/*******************************************************************************
 * Copyright (c) 2009 The Australian National University.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License v2.0
 *
 * @date $Date:$
 * @id $Id:$
 *******************************************************************************/
package org.dacapo.h2;

public class TPCCReporter {
  private int count = 0;
  
  public TPCCReporter() { }
  
  public synchronized void reset() {
    count = 0;
  }
  
  public synchronized void done() {
    count++;
    
    if ((count % 1000) == 0)
      System.out.print(".");
  }
}
