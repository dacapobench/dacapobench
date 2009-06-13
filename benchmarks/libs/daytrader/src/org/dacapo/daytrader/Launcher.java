package org.dacapo.daytrader;

import java.io.File;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

public class Launcher {
  private final static String GVERSION = "2.1.4";
  private final static String GTYPE = false ? "minimal" : "javaee5";
  private final static String GDIRECTORY = "geronimo-jetty6-"+GTYPE+"-"+GVERSION;
  private final static String CAR_NAME = "org.apache.geronimo.daytrader/daytrader-dacapo/2.2-SNAPSHOT/car";
  private final static String CLIENT_CLI_ENTRYPOINT = "org.dacapo.daytrader.DaCapoCLI";
  private final static String SERVER_CLI_ENTRYPOINT = "org.apache.geronimo.cli.daemon.DaemonCLI";
  private final static String[] CLIENT_BIN_JARS = { "client.jar"};
  private final static String[] SERVER_BIN_JARS = { "server.jar"};
  private final static String[] DACAPO_CLI_JAR = { "scratch/jar/daytrader.jar"};
  // The following list is defined in the "Class-Path:" filed of MANIFEST.MF for the client and server jars
  private final static String[] GERONIMO_LIB_JARS = { "geronimo-cli-"+GVERSION+".jar", "geronimo-kernel-"+GVERSION+".jar", "geronimo-transformer-"+GVERSION+".jar", "asm-3.1.jar", "asm-commons-3.1.jar", "commons-cli-1.0.jar", "commons-logging-1.0.4.jar", "cglib-nodep-2.1_3.jar", "log4j-1.2.14.jar", "xpp3-1.1.3.4.0.jar", "xstream-1.2.2.jar"};

  private static int numThreads = -1;
  private static String size;
  private static boolean useBeans = true;
  
  private static ClassLoader serverCLoader = null;
  private static ClassLoader clientCLoader = null;
  private static File scratch = null;

  private static ServerThread serverThread;
  
  public static void main(String[] args) {
    File scratch = new File((new File("./scratch")).getAbsolutePath());
    initialize(scratch, 16, "small", true);
    performIteration();
  }
  
  public static void initialize(File scratchdir, int threads, String dtSize, boolean beans) {
    numThreads = threads;
    size = dtSize;
    useBeans = beans;
    scratch = new File(scratchdir.getAbsolutePath());
    
    serverThread = new ServerThread();
    serverThread.start();
    invokeGeronimoClientCLI(getArgs(true));
  }
  
  public static void performIteration() {
    if (numThreads == -1) {
      System.err.println("Trying to run Daytrader before initializing.  Exiting.");
      System.exit(0);
    }
    invokeGeronimoClientCLI(getArgs(false));
  }
  
  private static String[] getArgs(boolean init) {
    int argCount = 5 + (init ? 1 : 0) + (useBeans ? 1: 0);
    String[] args = new String[argCount];
    args[0] = CAR_NAME;
    args[1] = "-t";
    args[2] = Integer.toString(numThreads);
    args[3] = "-s";
    args[4] = size;
    if (useBeans) {
      args[5] = "-b";
    }
    if (init) {
      args[argCount - 1] = "-i";
    }

   return args; 
  }
  
  static void invokeGeronimoClientCLI(String[] args) {
    try {
      setGeronimoProperties();
    
      if (clientCLoader == null)  clientCLoader = createGeronimoClassLoader(false);

      Thread.currentThread().setContextClassLoader(clientCLoader);

      Class<?> clazz = Class.forName(CLIENT_CLI_ENTRYPOINT, true, clientCLoader);
      Method method = clazz.getMethod("main", new Class[] { String[].class});
      method.invoke(null, new Object[] {args});
    } catch (Exception e) {
      System.err.print("Caught exception invoking client:"+e.toString());
      e.printStackTrace();
    }
  }
  
  static void startServer(String[] args) {
    try {
      setGeronimoProperties();
    
      if (serverCLoader == null) {
        serverCLoader = createGeronimoClassLoader(true);
      }
      Thread.currentThread().setContextClassLoader(serverCLoader);
      
      Class<?> clazz = Class.forName(SERVER_CLI_ENTRYPOINT, true, serverCLoader);
      Method method = clazz.getMethod("main", new Class[] { String[].class});
      method.invoke(null, new Object[] {args});
    } catch (Exception e) {
      System.err.print("Caught exception invoking server:"+e.toString());
      e.printStackTrace();
    }
  }
  
  private static void setGeronimoProperties() {
    File geronimo = new File(scratch, GDIRECTORY);
    System.setProperty("org.apache.geronimo.base.dir", geronimo.getPath());
    System.setProperty("java.ext.dirs", geronimo.getPath() + "/lib/ext:" +System.getProperty("java.home")+"/lib/ext");
    System.setProperty("java.io.tmpdir", geronimo.getPath() + "/var/temp");
    
  }
  
  private static ClassLoader createGeronimoClassLoader(boolean server) throws Exception {
    File geronimo = new File(scratch, GDIRECTORY);
    ClassLoader oldCL = Thread.currentThread().getContextClassLoader();
    ClassLoader binCL = createClassLoader(oldCL, getGeronimoBinaryJar(geronimo, server), false);
    ClassLoader libCL = createClassLoader(binCL, getGeronimoLibraryJars(geronimo, server), true);
    return libCL;
 }
  
  private static ClassLoader createClassLoader(ClassLoader oldCL, URL[] urls, boolean inverted) throws Exception {
    ClassLoader rtn = null;
    try {
      if (inverted) 
        rtn = new InvertedURLClassLoader(urls, oldCL);
      else
        rtn = new java.net.URLClassLoader(urls, oldCL);
    } catch (Exception e) {
      Thread.currentThread().setContextClassLoader(oldCL);
      System.err.println("Unable to create loader: ");
      e.printStackTrace();
      System.exit(-1);
    }
    return rtn;
  }
  
  /**
   * Get a list of jars (if any) which should be in the classpath for this benchmark
   *
   * @param config The config file for this benchmark, which lists the jars
   * @param scratch The scratch directory, in which the jars will be located
   * @return An array of URLs, one URL for each jar
   * @throws MalformedURLException
   */
  private static URL[] getGeronimoLibraryJars(File geronimo, boolean server) throws MalformedURLException {
    List jars = new java.util.ArrayList();

    if (server) {
      File endorsed = new File(geronimo, "lib/endorsed");
      addJars(jars, endorsed, endorsed.list());
    } else {
      File cwd = new File(".");
      addJars(jars, cwd, DACAPO_CLI_JAR);
    }
   
    File lib = new File(geronimo, "lib");
    addJars(jars, lib, GERONIMO_LIB_JARS);
    
    
    return (URL[]) jars.toArray(new URL[jars.size()]);
  }

  private static URL[] getGeronimoBinaryJar(File geronimo, boolean server) throws MalformedURLException {
    List jars = new java.util.ArrayList();
    addJars(jars, new File(geronimo, "bin"), (server ? SERVER_BIN_JARS : CLIENT_BIN_JARS ));
    return (URL[]) jars.toArray(new URL[jars.size()]);
  }
  
  /**
   * Get a list of jars (if any) which should be in the classpath for this benchmark
   *
   * @param config The config file for this benchmark, which lists the jars
   * @param scratch The scratch directory, in which the jars will be located
   * @return An array of URLs, one URL for each jar
   * @throws MalformedURLException
   */
  private static void addJars(List jars, File directory, String[] jarNames) throws MalformedURLException {
    if (jarNames != null) {
      for (int i = 0; i < jarNames.length; i++) {
        File jar = new File(directory, jarNames[i]);
        jars.add(jar.toURL());
      }
    }
  }
}
