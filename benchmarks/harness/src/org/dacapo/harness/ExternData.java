/*
 * Copyright (c) 2006, 2009 The Australian National University.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License v2.0.
 * You may obtain the license at
 * 
 *    http://www.opensource.org/licenses/apache2.0.php
 */
package org.dacapo.harness;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.FileOutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.nio.channels.Channels;
import java.util.Properties;

import javax.xml.bind.DatatypeConverter;

import java.security.MessageDigest;

public class ExternData {
  /**
   * Helper functions managing big data sets.
   * Using a "data installation directory" to keep
   * large data sets.
   */
  public static final Path DEFAULT_LOCAL_DACAPO_CONFIG = Paths.get(System.getProperty("user.home"), ".dacapo-config.properties");
  public static final String CONFIG_KEY_EXTERN_DATA_LOC = "Extern-Data-Location";
  public static final String DACAPO_DL_URL_RAW = "DaCapo-DL-URL-RAW";
  public static final String DACAPO_DL_COMMIT = "DaCapo-DL-Commit";

  private static String getDefaultLocation() {
    try {
      String parent = new File(ExternData.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent();
      return parent;
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
      return props.getProperty(CONFIG_KEY_EXTERN_DATA_LOC, getDefaultLocation());
    } catch (IOException ioe) {
      return getDefaultLocation();
    }
  }

  /**
   * Fail function when external data not found.
   */
  public static void failExtDataNotFound(String size, File extdata) {
    System.err.printf("ERROR: failed to find external data for size '%s'.\n", size);
    System.err.printf("Please check that you have installed the external data properly (current: %s)\n", extdata.getAbsolutePath());
    System.err.println("Please do one of the following:");
    System.err.println("  1) If you have not installed the large data, run DaCapo with --extdata-install");
    System.err.println("  2) If you have already installed the large data, run DaCapo with --extdata-set-location to correctly identify the location of the external data.");
    System.exit(-1);
  }
  public static void failExtJarNotFound(File extjar, File extdata) {
    System.err.printf("ERROR: failed to find jar: %s.\n", extjar.getName());
    System.err.printf("Please check that you have installed the external data properly (current: %s)\n", extdata.getAbsolutePath());
    System.err.println("Please do one of the following:");
    System.err.println("  1) If you have not installed the large data, run DaCapo with --extdata-install");
    System.err.println("  2) If you have already installed the large data, run DaCapo with --extdata-set-location to correctly identify the location of the external data.");
    System.exit(-1);
  }

  public static void setLocation(File path, boolean md5Check) {
    if (md5Check) {
      // MD5 check
      System.out.printf("Checking MD5 at %s...", path.toString());
      if (!checkExtDataDirMD5(path)) {
        System.out.println("failed!");
        System.err.println("WARNING: MD5 checking failed. Your huge data does not match expected release.");
        System.err.println("Please download and install the latest huge data using --extdata-install flag;");
        System.err.println("otherwise please note the changes in research publication.");
      } else
        System.out.println("done!");
    }

    File fileProperties  = new File(DEFAULT_LOCAL_DACAPO_CONFIG.toString());
    Properties props = new Properties();
    try {
      fileProperties.createNewFile();  // create new if does not exist
      props.load(new FileReader(fileProperties));
      props.setProperty(CONFIG_KEY_EXTERN_DATA_LOC, path.getAbsolutePath());
      props.store(new FileWriter(fileProperties), "");
      System.out.printf("External data location has been set at %s.\n", path.getAbsolutePath());
    } catch (IOException e) {
      System.err.printf("IOException when creating/reading file %s: %s\n",
        fileProperties.toString(), e.toString());
      System.exit(-1);
    }
  }

  private static boolean checkExtDataDirMD5(File dir) {
    InputStream in = ClassLoader.getSystemResourceAsStream("META-INF/huge-data-md5s.list");
    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
    File datDir = new File(dir, "dat");
    return reader.lines().map(l -> {
      String [] fields = l.split("  ");
      String md5Expected = fields[0];
      String filePath = fields[1];
      try {
        String md5 = getMD5(new File(datDir, filePath));
        if (!md5.toUpperCase().equals(md5Expected.toUpperCase())) {
          return false;
        }
      } catch (Exception e) {
        return false;
      }
      return true;
    }).reduce(true, (a, b) -> a & b);
  }

  /**
   * Download and install external data
   */
  public static void downloadAndInstall(File path) {
    try {
      // create directory if not exist already
      if (!path.exists())
        path.mkdirs();

      // download
      URL urlDLRawRoot = appendURL(new URL(TestHarness.getManifestAttribute(DACAPO_DL_URL_RAW)),
              TestHarness.getManifestAttribute(DACAPO_DL_COMMIT));
      BufferedReader dllistReader = new BufferedReader(new InputStreamReader(
              ClassLoader.getSystemResourceAsStream("META-INF/dlfiles.list")));
      dllistReader.lines().forEach(s -> {
        try {
          downloadAndExtractItem(s, urlDLRawRoot, path);
        } catch (Exception e) {
          System.err.println("Download external data failed.");
          System.err.println("Please try again.");
          System.exit(-1);
        }
      });

      setLocation(path, false);
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(-1);
    }
  }

  private static URL appendURL(URL url, String relPath) throws MalformedURLException{
    return new URL(url.toString() + "/" + relPath);
  }

  private static void downloadAndExtractItem(String itemRelPath, URL urlDLRawRoot, File localDataPath) throws Exception {
    URL urlItem = appendURL(urlDLRawRoot, itemRelPath);
    File fileLocalItem = new File(localDataPath, itemRelPath);

    if (!fileLocalItem.getParentFile().exists())
      fileLocalItem.getParentFile().mkdirs();

    System.out.printf("Downloading %s to %s...", urlItem.toString(), fileLocalItem.toString());
    try {
      ReadableByteChannel rbc = Channels.newChannel(urlItem.openStream());
      FileOutputStream fos = new FileOutputStream(fileLocalItem);
      fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
      fos.close();
      System.out.println("Done.");

      System.out.printf("Checking %s MD5...", fileLocalItem.toString());
      ReadableByteChannel rbcMD5 = Channels.newChannel(appendURL(urlDLRawRoot, itemRelPath + ".MD5").openStream());
      ByteBuffer buf = ByteBuffer.allocate(32); // MD5 is always 32 characters long
      rbcMD5.read(buf);
      String md5Expect = new String(buf.array(), Charset.forName("ASCII")).toLowerCase();
      String md5Actual = getMD5(fileLocalItem).toLowerCase();
      if (!md5Expect.equals(md5Actual)) {
        System.out.println("Failed!");
        System.err.printf("MD5 checking of %s failed: expect %s, got %s\n", fileLocalItem.toString(),
                md5Expect, md5Actual);
        System.exit(-1);
      }
      System.out.println("Done.");
      if (fileLocalItem.getName().endsWith(".zip")) {
        System.out.printf("Extracting %s...", fileLocalItem.toString());
        Benchmark.unpackZipStream(new BufferedInputStream(new FileInputStream(fileLocalItem)),
                fileLocalItem.getParentFile());
        System.out.println("Done.");
      }
    } catch (Exception e) {
      System.out.println("Failed!");
      e.printStackTrace();
      throw e;
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
