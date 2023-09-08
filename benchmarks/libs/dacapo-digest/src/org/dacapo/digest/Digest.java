/*
 * Copyright (c) 2006, 2009 The Australian National University.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License v2.0.
 * You may obtain the license at
 * 
 *    http://www.opensource.org/licenses/apache2.0.php
 */
package org.dacapo.digest;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Encapsulate the digests used for validation of files.
 * 
 * @date $Date: 2009-12-24 11:19:36 +1100 (Thu, 24 Dec 2009) $
 * @id $Id: Digest.java 738 2009-12-24 00:19:36Z steveb-oss $
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
}
