/*
 * Copyright (c) 2006, 2009 The Australian National University.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License v2.0.
 * You may obtain the license at
 * 
 *    http://www.opensource.org/licenses/apache2.0.php
 */
package org.dacapo.digest;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.security.MessageDigest;

/**
 * Perform a digest operation on a file. Also provides a 'main' method for
 * maintainers to use outside the harness.
 * 
 * Has facilities to canonicalise text files, using the following operations: -
 * Files are read using Java character-oriented I/O. This removes
 * platform-dependent CR/LF sequences - The absolute and relative paths of the
 * benchmark scratch directory are replaced with "$SCRATCH" - All occurrences of
 * "\" are replaced with "/" to bring windows filenames into line with Unix
 * ones. (A bit heavy handed, better alternatives are invited :)
 * 
 * @date $Date: 2009-12-24 11:19:36 +1100 (Thu, 24 Dec 2009) $
 * @id $Id: FileDigest.java 738 2009-12-24 00:19:36Z steveb-oss $
 */
public class FileDigest {

  private static final int BUFLEN = 8192;

  /**
   * Return a file checksum
   * 
   * @param file Name of file
   * @return The checksum
   * @throws IOException
   */
  public static byte[] get(String file, boolean isText, boolean filterScratch, File scratch) throws IOException {
    if (isText) {
      return getText(new File(file), filterScratch, scratch);
    } else {
      if (filterScratch) {
        System.err.println("ERROR: Cannot filter scratch paths in a binary file");
        // The return value should fail validation.
        return Digest.create().digest("ERROR: Cannot filter scratch paths in a binary file".getBytes());
      } else
        return getBinary(new File(file));
    }
  }

  /**
   * Replace all occurrences of a fixed string
   * 
   * @param line
   * @param substr
   * @param replacement
   * @return
   */
  private static String replaceAllFixed(String line, String substr, String replacement) {
    int start = 0;
    int match;
    while ((match = line.indexOf(substr, start)) != -1) {
      line = line.substring(0, match) + replacement + line.substring(match + substr.length(), line.length());
      start = match + replacement.length();
    }
    return line;
  }

  /**
   * Return a file checksum for a text file
   * 
   * @param file Name of file
   * @return The checksum
   * @throws IOException
   */
  public static byte[] getText(File file, boolean filter, File scratch) throws IOException {
    final MessageDigest digest = Digest.create();
    BufferedReader in = new BufferedReader(new FileReader(file));
    String line;
    int unvalidated = 0;
    while ((line = in.readLine()) != null) {
      if (filter) {
        if (line.startsWith("#NOVALIDATE#"))
          line = "#NOVALIDATE#" + (unvalidated++);
        line = replaceAllFixed(line, scratch.getAbsolutePath(), "$SCRATCH");
        line = replaceAllFixed(line, scratch.getPath(), "$SCRATCH");
        line = replaceAllFixed(line, "\\", "/");
      }
      byte[] buf = line.getBytes();
      for (int i = 0; i < buf.length; i++)
        digest.update(buf[i]);
    }
    in.close();
    return digest.digest();
  }

  /**
   * Return a file checksum for a binary file
   * 
   * @param file Name of file
   * @return The checksum
   * @throws IOException
   */
  public static byte[] getBinary(File file) throws IOException {
    final MessageDigest digest = Digest.create();
    BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));
    byte[] buf = new byte[BUFLEN];
    int len;
    while ((len = in.read(buf)) > 0) {
      for (int i = 0; i < len; i++)
        digest.update(buf[i]);
    }
    in.close();
    return digest.digest();
  }

  /**
   * Print out the digest of each file on the command line.
   * 
   * @param args
   */
  public static void main(String[] args) {
    try {
      boolean filterScratch = false;
      String scratchDir = "";
      boolean text = false;
      int i = 0;
      for (; i < args.length && args[i].charAt(0) == '-'; i++) {
        if (args[i].equals("-f")) {
          filterScratch = true;
          scratchDir = args[++i];
        } else if (args[i].equals("-t")) {
          text = true;
        } else {
          System.err.println("invalid flag " + args[i]);
          System.err.println("Usage: FileDigest [-t [-f scratchDir]] file...");
          System.exit(1);
        }
      }
      if (filterScratch && !text) {
        System.err.println("Can't filter scratch in binary input files");
        System.exit(2);
      }
      for (; i < args.length; i++)
        System.out.println(args[i] + " " + Digest.toString(get(args[i], text, filterScratch, new File(scratchDir))));
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

}
