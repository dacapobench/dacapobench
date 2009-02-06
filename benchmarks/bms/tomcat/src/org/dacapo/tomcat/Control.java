package org.dacapo.tomcat;

import java.io.File;
import java.io.IOException;

import org.apache.catalina.startup.Bootstrap;

/**
 * Class to encapsulate pre- and post-iteration startup etc.
 * 
 * Separated into a single class with a single public method for ease of
 * use via reflection.
 */
public class Control {
  /**
   * The Http port
   */
  protected static int port;

  /**
   * Setter for 'port'
   */
  public static void setPort(int port) {
    Control.port = port;
  }

  private final File scratch;

  public Control(File scratch, ClassLoader loader, int port) throws Exception {
    this.scratch = scratch;
    setPort(port);
  }
  
  /**
   * This method gets invoked reflexively from the Tomcat harness so as to
   * avoid classloader strangeness.
   * @param function
   * @throws Exception
   */
  public void exec(String function) throws Exception {
    if (function.equals("prepare")) {
      Bootstrap.main(new String[] {"startd"});
    } else if (function.equals("startIteration")) {
      startServer();
    } else if (function.equals("stopIteration")) {
      stopServer();
    } else if (function.equals("cleanup")) {
      Bootstrap.main(new String[] {"stopd"});
    } 
  }
  
  private void startServer() throws IOException {
    new StartApp("/examples").fetch(new File(scratch,"startApp.log"));
  }

  private void stopServer() throws IOException {
    new StopApp("/examples").fetch(new File(scratch,"stopApp.log"));
  }
  
}
