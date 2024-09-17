/*
 * Copyright (c) 2009 The Australian National University.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License v2.0.
 * You may obtain the license at
 * 
 *    http://www.opensource.org/licenses/apache2.0.php
 */
package org.dacapo.daytrader;

import java.io.File;
import java.nio.file.Files;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import org.dacapo.harness.util.AvailablePortFinder;

/** 
* date:  $Date: 2009-12-24 11:19:36 +1100 (Thu, 24 Dec 2009) $
* id: $Id: Launcher.java 738 2009-12-24 00:19:36Z steveb-oss $
*/
public class Launcher {
  private static final int MAX_THREADS = 128;   // Artificial ceiling due to issue #230
  private static final String TC_PROPERTY_STRING = "dacapo.daytrader.override"; // #230
  private static final int DEFAULT_WILDFLY_PORT = 8080;
  private static final int DEFAULT_PORT_OFFSET_STEP = 100;
  public static final int DAYTRADER_PORT = establishDaytraderPort();
  private final static String VERSION = "26.1.3";
  private final static String TYPE = "Final";
  private final static String DIRECTORY = "wildfly-" + VERSION + "." + TYPE;
  private final static String EAR = "daytrader-ear-3.0-SNAPSHOT.ear";

  // This jar contains the code that knows how to create and communicate with
  // geronimo environments
  private final static String[] DACAPO_CLI_JAR = { ".."+File.separator+".."+File.separator+".."+File.separator+"jar"+File.separator+"lib"+File.separator+"daytrader"+File.separator+"daytrader.jar" };

  // The following list is defined in the "Class-Path:" filed of MANIFEST.MF for the client and server jars
  private final static String[] WILDFLY_SERVER_JARS = {"jboss-modules.jar"};

  private static int numThreads = -1;
  private static int logNumSessions = 0;
  private static boolean useBeans = true;

  private static ClassLoader serverCLoader = null;
  private static File root = null;

  public static void initialize(File data, File scratch, int threads, int lns, boolean beans) {
    if (threads > MAX_THREADS) {
      if (System.getProperty(TC_PROPERTY_STRING) == null) {
        numThreads = MAX_THREADS;
        System.err.println("WARNING: Thread count reduced from " + threads + " to " + MAX_THREADS + ".");
        System.err.println("WARNING: Use '-D"+TC_PROPERTY_STRING+"=true' to override.");
        System.err.println("WARNING: See https://github.com/dacapobench/dacapobench/issues/230 for details.");
      } else {
        numThreads = threads;
        System.err.println("WARNING: Thread count has been overridden to exceed daytrader's recommended limit of " + MAX_THREADS + ".");
        System.err.println("WARNING: See https://github.com/dacapobench/dacapobench/issues/230 for details.");
      }
    } else
      numThreads = threads;
    logNumSessions = lns;
    useBeans = beans;
    root = new File(data.getAbsolutePath()+File.separator+"dat"+File.separator+"lib"+File.separator+"daytrader");

    setProperties(root, scratch, threads);
    setupFiles(scratch);

    ClassLoader originalCLoader = Thread.currentThread().getContextClassLoader();

    try {
      // Create a server environment (using server classloader)
      serverCLoader = createWildflyClassLoader(originalCLoader, true);
      Thread.currentThread().setContextClassLoader(serverCLoader);
      Class<?> clazz = serverCLoader.loadClass("org.dacapo.daytrader.DaCapoServerRunner");
      Method method = clazz.getMethod("initialize");
      method.invoke(null);

      // Create a client environment
      DaCapoClientRunner.initialize(logNumSessions, numThreads, false);
    } catch (Exception e) {
      System.err.println("Exception during initialization: " + e.toString());
      e.printStackTrace();
      Runtime.getRuntime().halt(1);
    } finally {
      Thread.currentThread().setContextClassLoader(originalCLoader);
    }
  }

  private static void setProperties(File data, File scratch, int threads) {
    System.setProperty("dacapo.client.threads", Integer.toString(threads));
    System.setProperty("dacapo.daytrader.ops", new File(root, "operations.csv").getPath());

    System.setProperty("jboss.home.dir", new File(root, DIRECTORY).getPath());
    System.setProperty("jboss.server.base.dir", scratch.getAbsolutePath());

    System.setProperty("module.path", new File(root, DIRECTORY + File.separator + "modules").getPath());
  }

  private static void setupFiles(File scratch) {
    try {
      File origsvr = new File(root.getAbsolutePath()+File.separator+DIRECTORY+File.separator+"standalone");

      File cfg = new File(scratch.getAbsolutePath()+File.separator+"configuration");
      File origcfg = new File(origsvr.getAbsolutePath()+File.separator+"configuration");
      if (!cfg.exists())
        cfg.mkdirs();
      for (String file : origcfg.list()) {
        File source = new File(origcfg.getAbsolutePath()+File.separator+file);
        File dest = new File(cfg.getAbsolutePath()+File.separator+file);
        Files.copy(source.toPath(), dest.toPath());
      }

      File dep = new File(scratch.getAbsolutePath()+File.separator+"deployments");
      if (!dep.exists())
        dep.mkdirs();
      File origdep = new File(origsvr.getAbsolutePath()+File.separator+"deployments");
      Files.copy(new File(origdep+File.separator+EAR).toPath(), new File(dep+File.separator+EAR).toPath());
    } catch (Exception e) {
      System.err.println("Exception while seting up files: " + e.toString());
      e.printStackTrace();
      Runtime.getRuntime().halt(1);
    }
  }

  public static void performPrepare() {
    if (numThreads == -1) {
      System.err.println("Trying to run Daytrader before initializing.  Exiting.");
      System.exit(0);
    }
    ClassLoader originalClassloader = Thread.currentThread().getContextClassLoader();
    try {
      Thread.currentThread().setContextClassLoader(serverCLoader);
      DaCapoClientRunner.prepare(logNumSessions, numThreads, useBeans);
    } catch (Exception e) {
      System.err.println("Exception during iteration: " + e.toString());
      e.printStackTrace();
    } finally {
      Thread.currentThread().setContextClassLoader(originalClassloader);
    }
  }


  public static void performIteration() {
    if (numThreads == -1) {
      System.err.println("Trying to run Daytrader before initializing.  Exiting.");
      System.exit(0);
    }
    ClassLoader originalClassloader = Thread.currentThread().getContextClassLoader();
    try {
      Thread.currentThread().setContextClassLoader(serverCLoader);
      DaCapoClientRunner.runIteration(logNumSessions, numThreads, 0, useBeans);
    } catch (Exception e) {
      System.err.println("Exception during iteration: " + e.toString());
      e.printStackTrace();
    } finally {
      Thread.currentThread().setContextClassLoader(originalClassloader);
    }
  }

  public static void shutdown(){
    try {
      Class<?> clazz = serverCLoader.loadClass("org.dacapo.daytrader.DaCapoServerRunner");
      Method method = clazz.getMethod("shutdown");
      method.invoke(null);
    }catch (Exception e) {
      System.err.println("Exception during iteration: " + e.toString());
      e.printStackTrace();
    }
  }

  /**
   * Create the classloader from within which to start the Geronimo client
   * and/or server kernels.
   * 
   * @param server Is this the server (or client) classloader
   * @return The classloader
   * @throws Exception
   */
  private static ClassLoader createWildflyClassLoader(ClassLoader parent, boolean server) {
    File wildfly = new File(root, DIRECTORY).getAbsoluteFile();

    return new URLClassLoader(getWildflyLibraryJars(wildfly, server), parent);
  }

  /**
   * Get a list of jars (if any) which should be in the library classpath for
   * this benchmark
   * 
   * @param wildfly The base directory for the jars
   * @return An array of URLs, one URL for each jar
   */
  private static URL[] getWildflyLibraryJars(File wildfly, boolean server) {
    List<URL> jars = new ArrayList<URL>();

    if (server) { 
        addJars(jars, wildfly, WILDFLY_SERVER_JARS);
    }

    addJars(jars, root, DACAPO_CLI_JAR);

    return jars.toArray(new URL[jars.size()]);
  }

  /**
   * Compile a list of paths to jars from a base directory and relative paths.
   * 
   * @param jars The url contain URL to jar files
   * @param directory The root directory, in which the jars will be located
   * @param jarNames The name of jar files to be added
   * @return An array of URLs, one URL for each jar
   */
  private static void addJars(List<URL> jars, File directory, String[] jarNames) {
    if (jarNames != null) {
      for (int i = 0; i < jarNames.length; i++) {
        File jar = new File(directory, jarNames[i]);
        try {
          URL url = jar.toURI().toURL();
          jars.add(url);
        } catch (MalformedURLException e) {
          System.err.println("Unable to create URL for jar: " + jarNames[i] + " in " + directory.toString());
          e.printStackTrace();
          System.exit(-1);
        }
      }
    }
  }

  static int establishDaytraderPort() {

    // determine whether a port offset was explicitly requested via command line property
    int offset = 0;
    String jbpo = System.getProperty("jboss.socket.binding.port-offset");
    if (jbpo != null) {
      try {
        offset = Integer.parseInt(jbpo);
      } catch (NumberFormatException e) {
        System.err.println("jboss.socket.binding.port-offset set to malformed value '"+jbpo+"', exiting.");
        System.exit(-1);
      }
    }

    // determine whether the http port was explicitly requested via command line property
    int port = DEFAULT_WILDFLY_PORT;
    String jbhp = System.getProperty("jboss.http.port");
    if (jbhp != null) {
      try {
        port = Integer.parseInt(jbhp);
      } catch (NumberFormatException e) {
        System.err.println("jboss.http.port set to malformed value '"+jbhp+"', exiting.");
        System.exit(-1);
      }
    } else
      System.setProperty("jboss.http.port", Integer.toString(port));

    // adjust the ports as necessary
    int requested = port + offset;
    if (!AvailablePortFinder.available(requested)) {
      int available = AvailablePortFinder.getNextAvailable(requested+DEFAULT_PORT_OFFSET_STEP);
      System.out.print("Port conflict detected.  Offset of "+(available - requested)+" will be applied.  ");
      offset += (available - requested);
      System.setProperty("jboss.socket.binding.port-offset", Integer.toString(offset));
    }

    System.out.println("DayTrader is using port "+(port+offset)+" ("+port+"+"+offset+"). Configure with -Djboss.http.port and/or -Djboss.socket.binding.port-offset");

    return port+offset;
  }
}