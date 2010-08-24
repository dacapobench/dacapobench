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
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;


public class Harness {
 
  // The DaCapo Harness is in a separate library within the dacapo.jar
  // allowing a separate class loader to be used for loading the dacapo harness
  private static final String  HARNESS_PATH   = "harness/";
  private static final String  HARNESS_CLASS  = "org.dacapo.harness.TestHarness";
  private static final String  HARNESS_METHOD = "main";
	
  public static void main(String[] args) throws ClassNotFoundException, SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
	ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
	
	URL harnessJarURL = classLoader.getResource(HARNESS_PATH);
	
	ClassLoader harnessClassLoader = new URLClassLoader(new URL[] { harnessJarURL });

	Thread.currentThread().setContextClassLoader(harnessClassLoader);
	
	Class dacapoHarnessClass = harnessClassLoader.loadClass(HARNESS_CLASS);

	Method harnessMain = dacapoHarnessClass.getDeclaredMethod(HARNESS_METHOD, String[].class);
	
	harnessMain.invoke(null, new Object[] { args });

	/* TestHarness.main(args); */
    System.exit(0);
  }
  
  
}
