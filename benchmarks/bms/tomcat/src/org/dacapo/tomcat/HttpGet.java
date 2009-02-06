package org.dacapo.tomcat;

import java.io.File;
import java.io.IOException;

import org.apache.commons.httpclient.methods.GetMethod;

/**
 * A page accessed via a GET method
 */
public class HttpGet extends Page {

  public HttpGet(Session session,String address) {
    this(session,address,200,null);
  }
  public HttpGet(Session session,String address, String digest) {
    this(session,address,200,digest);
  }
  public HttpGet(Session session,String address, int status) {
    this(session,address,status,null);
  }
  public HttpGet(Session session,String address, int status, String digest) {
    super(session,address,status,digest);
  }

  @Override
  public boolean fetch(File logFile, boolean keep) throws IOException {
    GetMethod get = new GetMethod(formatUrl());
    return fetch(get, logFile, keep);
  }
}
