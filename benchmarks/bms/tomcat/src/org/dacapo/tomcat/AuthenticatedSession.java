/*
 * Copyright (c) 2009 The Australian National University.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License v2.0.
 * You may obtain the license at
 * 
 *    http://www.opensource.org/licenses/apache2.0.php
 */
package org.dacapo.tomcat;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.params.HttpClientParams;

/**
 * An authenticated Tomcat session
 * 
 * @date $Date: 2009-12-04 14:33:59 +1100 (Fri, 04 Dec 2009) $
 * @id $Id: Slice.java 659 2009-12-04 03:33:59Z jzigman $
 */
public class AuthenticatedSession extends Session {

  private final String username;
  private final String password;

  /**
   * Factory method - forces correct initialization
   * 
   * @param username Username
   * @param password Password
   * @param port TCP port
   * @return The created session
   */
  public static Session create(String username, String password, int port) {
    return new AuthenticatedSession(username, password, port).init();
  }

  protected AuthenticatedSession(String username, String password, int port) {
    super(port);
    this.username = username;
    this.password = password;
  }

  @Override
  protected void setClientState(HttpState state) {
    super.setClientState(state);
    Credentials defaultcreds = new UsernamePasswordCredentials(username, password);
    state.setCredentials(new AuthScope("localhost", port, AuthScope.ANY_REALM), defaultcreds);
  }

  @Override
  protected void setClientParams(HttpClientParams params) {
    super.setClientParams(params);
    params.setAuthenticationPreemptive(true);
  }

}
