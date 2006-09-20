package dacapo;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Digest {
  public static MessageDigest create() {
    try {
      return MessageDigest.getInstance("SHA-1");
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
      System.exit(1);
      return null;
    }
  }
  
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
