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

public class ExternData {
  /**
   * Helper functions managing big data sets.
   * Using a "data installation directory" to keep
   * large data sets.
   */
  public static final String WORKING_DIRE = Paths.get("").toAbsolutePath().toString();
  public static final Path DEFAULT_LOCAL_DACAPO_CONFIG = Paths.get(System.getProperty("user.home"), ".dacapo-config.properties");
  public static final String CONFIG_KEY_EXTERN_DATA_LOC = "Extern-Data-Location";
  public static final String DACAPO_DL_URL_LFS = "DaCapo-DL-URL-LFS";
  public static final String DACAPO_DL_URL_RAW = "DaCapo-DL-URL-RAW";
  public static final String DACAPO_CHECKSUM_RE_PATH = "META-INF" + File.separator + "huge-data-md5s.list";

  static {
    disableSslVerification();
  }

  private static String getDefaultLocation() {
    try {
      String jar =  new File(ExternData.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getAbsolutePath();
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
      return props.getProperty(CONFIG_KEY_EXTERN_DATA_LOC, getDefaultLocation());
    } catch (IOException ioe) {
      return getDefaultLocation();
    }
  }

  /**
   * Fail function when external data not found.
   */
  public static void failExtDataNotFound(String size, File extdata, String file, String cfg) {
    System.err.println("ERROR: failed to find external data for size "+size);
    System.err.println("Expected to find: " + file);
    System.err.println("For config entry: " + cfg);
    System.err.printf("Please check that you have installed the external data properly (current: %s)\n", extdata == null ? "null" : extdata.getAbsolutePath());
    System.err.println("Please do one of the following:");
    System.err.println("  1) If you have not installed the large data, run DaCapo with [benchmark name] --extdata-install <dir-name>");
    System.err.println("  2) If you have already installed the large data, run DaCapo with --extdata-set-location <dir-name> to correctly identify the location of the external data.");
    System.exit(-1);
  }
  public static void failExtJarNotFound(File extjar, File extdata) {
    System.err.printf("ERROR: failed to find jar: %s.\n", extjar.getName());
    System.err.printf("Please check that you have installed the external jar package properly (current: %s)\n", extdata == null ? "null" : extdata.getAbsolutePath());
    System.err.println("Please do one of the following:");
    System.err.println("  1) If you have not installed the large data, run DaCapo with [benchmark name] --extdata-install <dir-name>");
    System.err.println("  2) If you have already installed the large data, run DaCapo with --extdata-set-location <dir-name> to correctly identify the location of the external data.");
    System.exit(-1);
  }

  public static void setLocation(File path, boolean md5Check) {
    if (md5Check) {
      downloadChecksum();
      // MD5 check
      System.out.printf("Checking MD5 at %s...", path.toString());
      if (!checkExtDataDirMD5(path)) {
        System.out.println("failed!");
        System.err.println("WARNING: MD5 check failed. Your data does not match expected release.");
        System.err.println("Please download and install the latest data using --extdata-install flag.");
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

  private static boolean downloadChecksum() {
    try {
      DataDownload.Download("META-INF"+File.separator+"huge-data-md5s.list", new File(WORKING_DIRE).getAbsolutePath(), "dat");
    } catch (Exception e) {
      return false;
    }
    return true;
  }

  private static boolean checkExtDataDirMD5(File dir){
    File checksum = new File(WORKING_DIRE, DACAPO_CHECKSUM_RE_PATH);
    InputStream in = ClassLoader.getSystemResourceAsStream("META-INF/huge-data-md5s.list");
    if(!checksum.exists()) {
      try {
        in = new FileInputStream(checksum);
      } catch (FileNotFoundException e) {
        e.printStackTrace();
      }
    }
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
  public static void downloadAndInstall(File path, String bench) {
    try {
      // create directory if not exist already
      if (!path.exists())
        path.mkdirs();

      // download
      BufferedReader dllistReader = new BufferedReader(new InputStreamReader(
              ClassLoader.getSystemResourceAsStream("META-INF/dlfiles.list")));

      ExecutorService executor = Executors.newCachedThreadPool();
      dllistReader.lines().forEach(s -> {
        try {
          if(bench.length() == 0 || s.startsWith("dat/"+bench) || s.startsWith("jar/"+bench)) {
            executor.submit(() -> {
              if (s.startsWith("jar")) {
                DataDownload.Download(s.split("/")[1], path.getAbsolutePath(), "jar");
              }
              if (s.startsWith("dat")) {
                DataDownload.Download(s.split("/")[1], path.getAbsolutePath(), "dat");
              }

              File fileLocalItem = new File(path, s);
              System.out.printf("Extracting %s...", fileLocalItem.toString());
              try {
                Benchmark.unpackZipStream(new BufferedInputStream(new FileInputStream(fileLocalItem)),
                              fileLocalItem.getParentFile());
              } catch (IOException e) {
                e.printStackTrace();
                System.exit(-1);
              }
              System.out.println("Done.");
              executor.shutdown();
            });
          }
        } catch(Exception e) {
          e.printStackTrace();
          System.exit(-1);
        }
      });

      executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MINUTES);
      setLocation(path, false);
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(-1);
    }
  }

  private static void downloadAndExtractItem(String itemRelPath, File path) throws Exception {
    // Create the directory
    path.mkdir();
    // download
    DataDownload.Download(itemRelPath, path.getAbsolutePath());
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

  private static void disableSslVerification() {
    try
    {
      // Create a trust manager that does not validate certificate chains
      TrustManager[] trustAllCerts = new TrustManager[] {new X509TrustManager() {
        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
          return null;
        }
        public void checkClientTrusted(X509Certificate[] certs, String authType) {
        }
        public void checkServerTrusted(X509Certificate[] certs, String authType) {
        }
      }
      };

      // Install the all-trusting trust manager
      SSLContext sc = SSLContext.getInstance("SSL");
      sc.init(null, trustAllCerts, new java.security.SecureRandom());
      HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

      // Create all-trusting host name verifier
      HostnameVerifier allHostsValid = new HostnameVerifier() {
        public boolean verify(String hostname, SSLSession session) {
          return true;
        }
      };

      // Install the all-trusting host verifier
      HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
    } catch (KeyManagementException e) {
      e.printStackTrace();
    }
  }
}
