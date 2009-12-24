/*
 * Copyright (c) 2006, 2009 The Australian National University.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License v2.0.
 * You may obtain the license at
 * 
 *    http://www.opensource.org/licenses/apache2.0.php
 */
package org.dacapo.tomcat;

import java.io.File;
import java.io.IOException;

import org.apache.commons.httpclient.methods.GetMethod;

/**
 * A page accessed via a GET method
 * 
 * @date $Date: 2009-12-24 11:19:36 +1100 (Thu, 24 Dec 2009) $
 * @id $Id: HttpGet.java 738 2009-12-24 00:19:36Z steveb-oss $
 */
public class HttpGet extends Page {

  /**
   * An HTTP Get request for the URL {@code address}. Expects an HTTP return
   * code of 200 and does not perform digest validation.
   * 
   * @param address The URL (server-relative)
   */
  public HttpGet(String address) {
    this(address, 200, null);
  }

  /**
   * An HTTP Get request for the URL {@code address}. Expects an HTTP return
   * code of 200 and performs digest validation.
   * 
   * @param address The URL (server-relative)
   * @param digest The expected digest
   */
  public HttpGet(String address, String digest) {
    this(address, 200, digest);
  }

  /**
   * An HTTP Get request for the URL {@code address}. Does not perform digest
   * validation.
   * 
   * @param address The URL (server-relative)
   * @param status The expected HTTP status
   */
  public HttpGet(String address, int status) {
    this(address, status, null);
  }

  /**
   * An HTTP Get request for the URL {@code address}.
   * 
   * @param address The URL (server-relative)
   * @param status The expected HTTP status
   * @param digest The expected digest
   */
  public HttpGet(String address, int status, String digest) {
    super(address, status, digest);
  }

  /**
   * @see org.dacapo.tomcat.Page#fetch(org.dacapo.tomcat.Session, java.io.File,
   * boolean)
   */
  @Override
  public boolean fetch(Session session, File logFile, boolean keep) throws IOException {
    return fetch(session, new GetMethod(formatUrl(session)), logFile, keep);
  }
}
