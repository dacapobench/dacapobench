/*
 * Copyright (c) 2006, 2009 The Australian National University.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License v2.0.
 * You may obtain the license at
 * 
 *    http://www.opensource.org/licenses/apache2.0.php
 */
package org.dacapo.harness;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Encapsulate the digests used for validation of files.
 * 
 * date:  $Date: 2009-12-24 11:19:36 +1100 (Thu, 24 Dec 2009) $
 * id: $Id: Digest.java 738 2009-12-24 00:19:36Z steveb-oss $
 */
public class Digest {

  /**
   * Return an instance of our chosen message digest
   * 
   * @return The MessageDigest object
   */
  public static MessageDigest create() {
    try {
      return MessageDigest.getInstance("SHA-1");
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
      System.exit(1);
      return null;
    }
  }

  /**
   * String representation of a message digest.
   * 
   * @param digest
   * @return
   */
  public static String toString(byte[] digest) {
    StringBuffer result = new StringBuffer(digest.length * 2);
    for (int i = 0; i < digest.length; i++) {
      String s = Integer.toHexString(((int) digest[i]) & 0xFF);
      if (s.length() == 1)
        result.append("0");
      result.append(s);
    }
    return result.toString();
  }

  /**
   * Calculate the SHA-1 digest of a given string, and return a string
   * representation thereof
   * 
   * @param str The string to be checksummed
   * @return A string representation of the SHA-1 digest
   */
  public static String stringDigest(String str) {
    // note we use str.length() for backward compatability (there twice that number of bytes)
    return byteDigest(str.getBytes(), str.length());
  }

  /**
   * Calculate the SHA-1 digest of a given string, and return a string
   * representation thereof
   * 
   * @param str The string to be checksummed
   * @param max Only consider the first N characters
   * @return A string representation of the SHA-1 digest
   */
  public static String stringDigest(String str, int max) {
    // note we use str.length() for backward compatability (there twice that number of bytes)
    return byteDigest(str.getBytes(), Math.min(max, str.length()));
  }

  /**
   * Calculate the SHA-1 digest of a given byte array, and return a string
   * representation thereof
   * 
   * @param b The byte array to be checksummed
   * @return A string representation of the SHA-1 digest
   */
  public static String byteDigest(byte[] b) {
    return byteDigest(b, b.length);
  }

  /**
   * Calculate the SHA-1 digest of a given byte array, and return a string
   * representation thereof
   * 
   * @param b The byte array to be checksummed
   * @param max Only consider the first N bytes
   * @return A string representation of the SHA-1 digest
   */
  public static String byteDigest(byte[] b, int max) {
    final MessageDigest md = create();
    for (int i = 0; i < Math.min(b.length, max); i++) {
      md.update(b[i]);
    }
    return toString(md.digest()); // Must only ever call digest() once!
  }
}
