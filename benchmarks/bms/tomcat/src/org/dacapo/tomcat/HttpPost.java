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

import org.apache.commons.httpclient.methods.PostMethod;

/**
 * A page accessed via a POST method
 */
public class HttpPost extends Page {

  /**
   * An HTTP POST request for the URL {@code address}. Expects an HTTP return
   * code of 200 and does not perform digest validation.
   * 
   * @param address
   *          The URL (server-relative)
   */
  public HttpPost(String address) {
    this(address, 200, null);
  }

  /**
   * An HTTP POST request for the URL {@code address}. Expects an HTTP return
   * code of 200 and performs digest validation.
   * 
   * @param address
   *          The URL (server-relative)
   * @param digest
   *          The expected digest
   */
  public HttpPost(String address, String digest) {
    this(address, 200, digest);
  }

  /**
   * An HTTP POST request for the URL {@code address}. Does not perform digest
   * validation.
   * 
   * @param address
   *          The URL (server-relative)
   * @param status
   *          The expected HTTP status
   */
  public HttpPost(String address, int status) {
    this(address, status, null);
  }

  /**
   * An HTTP POST request for the URL {@code address}.
   * 
   * @param address
   *          The URL (server-relative)
   * @param status
   *          The expected HTTP status
   * @param digest
   *          The expected digest
   */
  public HttpPost(String address, int status, String digest) {
    super(address, status, digest);
  }

  @Override
  protected boolean fetch(Session session, File logFile, boolean keep)
      throws IOException {
    PostMethod post = new PostMethod(formatUrl(session));
    return fetch(session, post, logFile, keep);
  }
}
