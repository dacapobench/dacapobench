/*
 * Copyright (c) 2006, 2009 The Australian National University.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License v2.0.
 * You may obtain the license at
 * 
 *    http://www.opensource.org/licenses/apache2.0.php
 */
package org.dacapo.tomcat;

/**
 * Request tomcat to start serving the named application
 * 
 * @date $Date: 2009-12-04 14:33:59 +1100 (Fri, 04 Dec 2009) $
 * @id $Id: Slice.java 659 2009-12-04 03:33:59Z jzigman $
 */
public class StartApp extends HttpGet {

  /**
   * @param path The path to the webapp to start
   */
  public StartApp(String path) {
    super("/manager/start?path="+path,200,null);
  }
}
