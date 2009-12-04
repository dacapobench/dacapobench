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
 * Request tomcat to stop serving the named application
 */
public class StopApp extends HttpGet {

  /**
   * @param path
   *          The path to the webapp to stop
   */
  public StopApp(String path) {
    super("/manager/stop?path=" + path, 200, null);
  }
}
