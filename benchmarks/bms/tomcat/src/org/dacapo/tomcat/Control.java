/*
 * Copyright (c) 2009 The Australian National University.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License v2.0.
 * You may obtain the license at
 * 
 *    http://www.opensource.org/licenses/apache2.0.php
 */
package org.dacapo.tomcat;

import java.io.File;
import java.io.IOException;

import org.apache.catalina.startup.Bootstrap;

/**
 * Class to encapsulate pre- and post-iteration startup etc.
 * 
 * Separated into a single class with a single public method for ease of use via
 * reflection.
 * 
 * @date $Date: 2009-12-24 11:19:36 +1100 (Thu, 24 Dec 2009) $
 * @id $Id: Control.java 738 2009-12-24 00:19:36Z steveb-oss $
 */
public class Control {
  /**
   * The Http port
   */
  private final int port;

  private final File scratch;

  /**
   * @param scratch The scratch directory
   * @param loader The class loader
   * @param port The network port
   */
  public Control(File scratch, ClassLoader loader, int port) {
    this.scratch = scratch;
    this.port = port;
  }

  /**
   * This method gets invoked reflectively from the Tomcat harness so as to
   * avoid classloader strangeness. It would be more correct to use an enum for
   * the 'function' parameter, but we have classloader issues, so a string makes
   * life easier.
   * 
   * @param function The function to perform , one of
   * "prepare","startIteration", "stopIteration","cleanup".
   * @throws Exception Passed back from the Tomcat bootstrap
   */
  public void exec(String function) throws Exception {
    if (function.equals("prepare")) {
      Bootstrap.main(new String[] { "startd" });
    } else if (function.equals("startIteration")) {
      startServer();
    } else if (function.equals("stopIteration")) {
      stopServer();
    } else if (function.equals("cleanup")) {
      Bootstrap.main(new String[] { "stopd" });
    }
  }

  private void startServer() throws IOException {
    new StartApp("/examples").fetch(AuthenticatedSession.create("tomcat", "s3cret", port), new File(scratch, "startApp.log"));
  }

  private void stopServer() throws IOException {
    new StopApp("/examples").fetch(AuthenticatedSession.create("tomcat", "s3cret", port), new File(scratch, "stopApp.log"));
  }

}
