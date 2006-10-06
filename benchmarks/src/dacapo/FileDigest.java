package dacapo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.security.DigestInputStream;
import java.security.MessageDigest;

/**
 * Perform a digest operation on a file.  Provides a main for maintainers
 * to use outside the harness.
 * 
 * @author Robin Garner
 * @date $Date: 2006-10-06 12:19:08 +1000 (Fri, 06 Oct 2006) $
 * @id $Id: FileDigest.java 138 2006-10-06 02:19:08Z rgarner $
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
      BufferedReader in = new BufferedReader(new FileReader(file));
      String line;
      while ((line = in.readLine()) != null) {
        byte[] buf = line.getBytes();
        for (int i=0; i < buf.length; i++)
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
      for (int i=0; i < args.length; i++)
        System.out.println(Digest.toString(get(args[i])));
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

}
