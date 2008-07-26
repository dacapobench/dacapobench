/*
 * 
 */
package org.dacapo.harness;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Encapsulate the digests used for validation of files.
 * 
 * @author Robin Garner
 * @date $Date: 2008-07-26 11:23:30 +1000 (Sat, 26 Jul 2008) $
 * @id $Id: Digest.java 379 2008-07-26 01:23:30Z steveb-oss $
 *
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
    StringBuffer result = new StringBuffer(digest.length*2);
    for (int i=0; i < digest.length; i++) {
      String s = Integer.toHexString(((int)digest[i])&0xFF);
      if (s.length() == 1)
        result.append("0");
      result.append(s);
    }
    return result.toString();
  }
}
