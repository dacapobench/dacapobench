/*******************************************************************************
 * Copyright (c) 2005, 2009 The Australian National University.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License v2.0
 *
 * @date $Date:$
 * @id $Id:$
 *******************************************************************************/
package org.dacapo.tomcat;

import java.io.File;
import java.io.IOException;

import org.apache.commons.httpclient.methods.GetMethod;

/**
 * A page accessed via a GET method
 */
public class HttpGet extends Page {

  /**
   * An HTTP Get request for the URL {@code address}. Expects an HTTP return
   * code of 200 and does not perform digest validation.
   * 
   * @param address
   *          The URL (server-relative)
   */
  public HttpGet(String address) {
    this(address, 200, null);
  }

  /**
   * An HTTP Get request for the URL {@code address}. Expects an HTTP return
   * code of 200 and performs digest validation.
   * 
   * @param address
   *          The URL (server-relative)
   * @param digest
   *          The expected digest
   */
  public HttpGet(String address, String digest) {
    this(address, 200, digest);
  }

  /**
   * An HTTP Get request for the URL {@code address}. Does not perform digest
   * validation.
   * 
   * @param address
   *          The URL (server-relative)
   * @param status
   *          The expected HTTP status
   */
  public HttpGet(String address, int status) {
    this(address, status, null);
  }

  /**
   * An HTTP Get request for the URL {@code address}.
   * 
   * @param address
   *          The URL (server-relative)
   * @param status
   *          The expected HTTP status
   * @param digest
   *          The expected digest
   */
  public HttpGet(String address, int status, String digest) {
    super(address, status, digest);
  }

  /**
   * @see org.dacapo.tomcat.Page#fetch(org.dacapo.tomcat.Session, java.io.File,
   *      boolean)
   */
  @Override
  public boolean fetch(Session session, File logFile, boolean keep)
      throws IOException {
    return fetch(session, new GetMethod(formatUrl(session)), logFile, keep);
  }
}
