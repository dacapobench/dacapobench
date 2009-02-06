package org.dacapo.tomcat;

import java.io.File;
import java.io.IOException;

import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.PostMethod;

/**
 * A page accessed via a POST method
 */
public class HttpPost extends Page {

  public HttpPost(Session session,String address) {
    this(session,address,200,null);
  }
  public HttpPost(Session session,String address, String digest) {
    this(session,address,200,digest);
  }
  public HttpPost(Session session,String address, int status) {
    this(session,address,status,null);
  }
  public HttpPost(Session session,String address, int status, String digest) {
    super(session,address,status,digest);
  }

  @Override
  protected boolean fetch(File logFile, boolean keep) throws IOException {
    PostMethod post = new PostMethod(formatUrl());
    return fetch(post, logFile, keep);
  }
  
//  protected boolean fetch(File logFile, NameValuePair...params) throws IOException {
//    PostMethod post = new PostMethod(formatUrl());
//    for (NameValuePair p : params) {
//      post.addParameter(p);
//    }
//    return fetch(post, logFile);
//  }
}
