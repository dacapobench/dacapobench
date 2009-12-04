/*******************************************************************************
 * Copyright (c) 2005, 2009 The Australian National University.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License v2.0
 *
 * @date $Date:$
 * @id $Id:$
 *******************************************************************************/
package org.dacapo.tomcat;

/**
 * Request tomcat to start serving the named application
 */
public class StartApp extends HttpGet {

  /**
   * @param path
   *          The path to the webapp to start
   */
  public StartApp(String path) {
    super("/manager/start?path=" + path, 200, null);
  }
}
