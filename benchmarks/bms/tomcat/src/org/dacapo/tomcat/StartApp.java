package org.dacapo.tomcat;

import java.net.URLEncoder;

/**
 * Request tomcat to start serving the named application
 */
public class StartApp extends HttpGet {

  public StartApp(String path) {
    super(AuthenticatedSession.create("tomcat","s3cret"),"/manager/start?path="+path,200,null);
  }

  private static String urlEncode(String path) {
    try {
      return URLEncoder.encode(path,"utf-8");
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
