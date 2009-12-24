/*
 * Copyright (c) 2006, 2009 The Australian National University.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License v2.0.
 * You may obtain the license at
 * 
 *    http://www.opensource.org/licenses/apache2.0.php
 */
package org.dacapo.harness;

/**
 * @date $Date: 2009-12-24 11:19:36 +1100 (Thu, 24 Dec 2009) $
 * @id $Id: DacapoException.java 738 2009-12-24 00:19:36Z steveb-oss $
 */
public class DacapoException extends Exception {
  public DacapoException(String text) {
    super(text);
  }

  public DacapoException(Exception e) {
    super(e);
  }

  private static final long serialVersionUID = 3726765834685635667l;
}
