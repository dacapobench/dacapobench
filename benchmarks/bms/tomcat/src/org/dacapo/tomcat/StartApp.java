package org.dacapo.tomcat;

/**
 * Request tomcat to start serving the named application
 */
public class StartApp extends HttpGet {

  /**
   * @param path The path to the webapp to start
   */
  public StartApp(String path) {
    super("/manager/start?path="+path,200,null);
  }
}
