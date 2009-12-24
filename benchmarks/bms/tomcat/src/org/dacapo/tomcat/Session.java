/*
 * Copyright (c) 2006, 2009 The Australian National University.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License v2.0.
 * You may obtain the license at
 * 
 *    http://www.opensource.org/licenses/apache2.0.php
 */
package org.dacapo.tomcat;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;

/**
 * Per-session data
 * 
 * @date $Date: 2009-12-24 11:19:36 +1100 (Thu, 24 Dec 2009) $
 * @id $Id: Session.java 738 2009-12-24 00:19:36Z steveb-oss $
 */
public class Session {

  /**
   * The underlying HttpClient object
   */
  protected final MultiThreadedHttpConnectionManager connectionManager = new MultiThreadedHttpConnectionManager();
  protected final HttpClient httpClient = new HttpClient(connectionManager);
  protected final int port;

  /**
   * @param port TCP port
   * @return A correctly initialized Session
   */
  public static Session create(int port) {
    return new Session(port).init();
  }

  protected Session(int port) {
    this.port = port;
  }

  /**
   * Allow post-constructor initialization. The method signature allows us to do
   * Session session = new Session().init();
   * 
   * Separating initialization from construction allows subclasses to perform
   * initialization that depends on constructor arguments.
   * 
   * @return The instance, initialized
   */
  protected <T extends Session> T init() {
    setConnectionManagerParams(httpClient.getHttpConnectionManager().getParams());
    setClientParams(httpClient.getParams());
    setClientState(httpClient.getState());
    @SuppressWarnings("unchecked")
    T result = (T) this;
    return result;
  }

  /**
   * Set connection manager parameters.
   * @param params
   */
  protected void setConnectionManagerParams(HttpConnectionManagerParams params) {
    params.setConnectionTimeout(30000);
  }

  protected void setClientParams(HttpClientParams params) {
  }

  protected void setClientState(HttpState state) {
  }

  /**
   * @return The TCP port for this session
   */
  public int getPort() {
    return port;
  }

  /**
   * shutdown the session cleanup all resources
   */
  public void shutdown() {
    connectionManager.shutdown();
  }
}
