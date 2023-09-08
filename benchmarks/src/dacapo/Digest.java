/*
 * 
 */
package dacapo;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Encapsulate the digests used for validation of files.
 * 
 * @author Robin Garner
 * @date $Date: 2006-09-28 04:15:21 +0000 (Thu, 28 Sep 2006) $
 * @id $Id: Digest.java 134 2006-09-28 04:15:21 +0000 (Thu, 28 Sep 2006) rgarner $
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
