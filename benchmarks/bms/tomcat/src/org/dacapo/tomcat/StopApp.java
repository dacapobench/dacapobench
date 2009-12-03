package org.dacapo.tomcat;

/**
 * Request tomcat to stop serving the named application
 */
public class StopApp extends HttpGet {

  /**
   * @param path
   *          The path to the webapp to stop
   */
  public StopApp(String path) {
    super("/manager/stop?path=" + path, 200, null);
  }
}
