package dacapo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.DigestInputStream;
import java.security.MessageDigest;

/**
 * Perform a digest operation on a file.  Provides a main for maintainers
 * to use outside the harness.
 * 
 * @author Robin Garner
 * @date $Date: 2006-09-28 21:11:06 +1000 (Thu, 28 Sep 2006) $
 * @id $Id: FileDigest.java 135 2006-09-28 11:11:06Z rgarner $
 *
 */
public class FileDigest {
  
  /**
   * Return a file checksum
   * 
   * @param file Name of file
   * @return The checksum
   * @throws IOException
   */
  public static byte[] get(String file) throws IOException { return get(new File(file)); }

  /**
   * Return a file checksum
   * 
   * @param file Name of file
   * @return The checksum
   * @throws IOException
   */
  public static byte[] get(File dir, String file) throws IOException { return get(new File(dir,file)); }
  
  /**
   * Return a file checksum
   * 
   * @param file Name of file
   * @return The checksum
   * @throws IOException
   */
  public static byte[] get(File file) throws IOException {
      final MessageDigest digest = Digest.create();
      DigestInputStream in = new DigestInputStream(new FileInputStream(file),digest);
      byte[] buffer = new byte[2048];
      while (in.read(buffer) >= 0) 
        ;
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
      for (int i=0; i < args.length; i++)
        System.out.println(Digest.toString(get(args[i])));
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

}
