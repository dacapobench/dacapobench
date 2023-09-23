/*
 * Copyright (c) 2006, 2009 The Australian National University.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License v2.0.
 * You may obtain the license at
 *
 *    http://www.opensource.org/licenses/apache2.0.php
 */
package org.dacapo.harness;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.Channels;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.Properties;

import javax.net.ssl.*;
import javax.xml.bind.DatatypeConverter;

import java.security.MessageDigest;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Data {
  /**
   * Helper functions managing data.
   */
  public static final String WORKING_DIRE = Paths.get("").toAbsolutePath().toString();
  public static final Path DEFAULT_LOCAL_DACAPO_CONFIG = Paths.get(System.getProperty("user.home"), ".dacapo-config.properties");
  public static final String CONFIG_KEY_DATA_LOC = "Data-Location";
  public static final String DACAPO_CHECKSUM_RE_PATH = "META-INF" + File.separator + "huge-data-md5s.list";

  private static String getDefaultLocation() {
    try {
      String jar =  new File(Data.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getAbsolutePath();
      return jar.replace(".jar","");
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(-1);
      return null;  // to satisfy the static checker
    }
  }

  /**
   * Retrieve data installation directory from $HOME/.dacapo-config.properties
   * If doesn't exist, return the default path (/path/to/dacapo.jar/../)
   */
  public static String getLocation() {
    File fileProperties  = new File(DEFAULT_LOCAL_DACAPO_CONFIG.toString());
    Properties props = new Properties();
    try {
      props.load(new FileReader(fileProperties));
      return props.getProperty(CONFIG_KEY_DATA_LOC, getDefaultLocation());
    } catch (IOException ioe) {
      return getDefaultLocation();
    }
  }

  public static void checkData(File path) {
    if (!path.exists())
      failDataNotFound(path);
  }

  /**
   * Fail function when data not found.
   */
  public static void failDataNotFound(File path) {
    System.err.printf("FATAL ERROR: Failed to find data at %s"+System.lineSeparator(), path == null ? "null" : path.getAbsolutePath());
    System.err.println();
    System.err.println("Please run DaCapo with --data-set-location <parent-dir-name> to reset the location of the parent directory.");
    System.exit(-1);
  }

  public static void setLocation(File path, boolean md5Check) {
    File fileProperties  = new File(DEFAULT_LOCAL_DACAPO_CONFIG.toString());
    Properties props = new Properties();
    try {
      fileProperties.createNewFile();  // create new if does not exist
      props.load(new FileReader(fileProperties));
      props.setProperty(CONFIG_KEY_DATA_LOC, path.getAbsolutePath());
      props.store(new FileWriter(fileProperties), "");
      System.out.printf("Data location has been set at %s.\n", path.getAbsolutePath());
    } catch (IOException e) {
      System.err.printf("IOException when creating/reading file %s: %s\n",
        fileProperties.toString(), e.toString());
      System.exit(-1);
    }
  }

  private static String getMD5(File file) throws Exception{
      MessageDigest md = MessageDigest.getInstance("MD5");
      byte [] buffer = new byte [1024 * 1024 * 32]; // 32 MB buffer
      InputStream stream = Files.newInputStream(file.toPath());
      int bytesRead = 0;
      while ((bytesRead = stream.read(buffer)) != -1) {
        md.update(buffer, 0, bytesRead);
      }
      return DatatypeConverter.printHexBinary(md.digest());
  }
}
