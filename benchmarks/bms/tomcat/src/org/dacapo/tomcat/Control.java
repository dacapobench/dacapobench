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
  private final int port;

  private final File scratch;

  /**
   * @param scratch The scratch directory
   * @param loader The class loader
   * @param port The network port
   */
  public Control(File scratch, ClassLoader loader, int port) {
    this.scratch = scratch;
    this.port = port;
  }

  /**
   * This method gets invoked reflectively from the Tomcat harness so as to
   * avoid classloader strangeness.  It would be more correct to use an enum for
   * the 'function' parameter, but we have classloader issues, so a string makes
   * life easier.
   *
   * @param function The function to perform , one of "prepare","startIteration",
   *   "stopIteration","cleanup".
   * @throws Exception Passed back from the Tomcat bootstrap
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
    new StartApp("/examples").fetch(AuthenticatedSession.create("tomcat","s3cret",port),new File(scratch,"startApp.log"));
  }

  private void stopServer() throws IOException {
    new StopApp("/examples").fetch(AuthenticatedSession.create("tomcat","s3cret",port),new File(scratch,"stopApp.log"));
  }

}
