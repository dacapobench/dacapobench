/*
 * Copyright (c) 2005, 2009 The Australian National University.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License v2.0.
 * You may obtain the license at
 * 
 *    http://www.opensource.org/licenses/apache2.0.php
 * @date $Date: 2009-12-24 11:19:36 +1100 (Thu, 24 Dec 2009) $
 * @id $Id: Harness.java 738 2009-12-24 00:19:36Z steveb-oss $
 */
import java.io.File;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;

public class Harness {

  // The DaCapo Harness is in a separate library within the dacapo.jar
  // allowing a separate class loader to be used for loading the dacapo harness
  private static final String HARNESS_PATH = "harness/";
  private static final String HARNESS_CLASS = "org.dacapo.harness.TestHarness";
  private static final String HARNESS_METHOD = "main";

  //
  private static final String CALLBACK_CLASSPATH_PROPERTY = "dacapo.callback.classpath";

  public static void main(String[] args) throws Exception {
    ClassLoader harnessClassLoader = makeHarnessClassLoader();

    Thread.currentThread().setContextClassLoader(harnessClassLoader);

    Class dacapoHarnessClass = harnessClassLoader.loadClass(HARNESS_CLASS);

    Method harnessMain = dacapoHarnessClass.getDeclaredMethod(HARNESS_METHOD, String[].class);

    harnessMain.invoke(null, new Object[] { args });

    System.exit(0);
  }

  private static ClassLoader makeHarnessClassLoader() throws MalformedURLException, URISyntaxException {
    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    String dacapoCallbackClasspath = System
        .getProperty(CALLBACK_CLASSPATH_PROPERTY);

    URL harnessJarURL = classLoader.getResource(HARNESS_PATH);

    URL[] urls = null;

    if (dacapoCallbackClasspath != null) {
      File file = new File(dacapoCallbackClasspath);
      URI callbackClasspath = null;
        
      if (file.isDirectory())
        callbackClasspath = file.getAbsoluteFile().toURI();
      else
        callbackClasspath = new URI(dacapoCallbackClasspath);

      if (callbackClasspath == null)
        throw new URISyntaxException(dacapoCallbackClasspath, "is not a URI nor a directory nor a class file");
      
      urls = new URL[] { harnessJarURL, callbackClasspath.toURL() };
    } else
      urls = new URL[] { harnessJarURL };

    return URLClassLoader.newInstance(urls, classLoader);
  }
}
