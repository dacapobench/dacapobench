package dacapo;

import java.io.BufferedInputStream;
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
 * @date $Date: 2006-10-06 19:15:50 +1000 (Fri, 06 Oct 2006) $
 * @id $Id: FileDigest.java 142 2006-10-06 09:15:50Z rgarner $
 *
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
  public static byte[] get(String file, boolean isText, 
      boolean filterScratch, File scratch) throws IOException { 
    if (isText) {
      return getText(new File(file),filterScratch,scratch); 
    } else {
      if (filterScratch) {
        System.err.println("ERROR: Cannot filter scratch paths in a binary file");
        // The return value should fail validation.
        return Digest.create().digest("ERROR: Cannot filter scratch paths in a binary file".getBytes());
      } else 
        return getBinary(new File(file));
    }
  }
  
  private static String replaceAllFixed(String line, String substr, String replacement) {
    int start = 0;
    int match;
    while ((match=line.indexOf(substr,start)) != -1) {
      line = line.substring(0, match) 
      + replacement + line.substring(match+substr.length(),line.length());
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
      while ((line = in.readLine()) != null) {
        if (filter) {
          line = replaceAllFixed(line,scratch.getAbsolutePath(), "$SCRATCH");
          line = replaceAllFixed(line,scratch.getPath(), "$SCRATCH");
        }
        byte[] buf = line.getBytes();
        for (int i=0; i < buf.length; i++)
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
      while ((len=in.read()) > 0) {
        for (int i=0; i < len; i++)
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
      for (int i=0; i < args.length && args[i].charAt(0) == '-'; i++)
        if (args[i].equals("-f")) {
          filterScratch = true;
          scratchDir = args[++i];
        } else if (args[i].equals("-t")) {
          text = true;
        } else {
          System.err.println("invalid flag "+args[i]);
          System.err.println("Usage: FileDigest [-t [-f scratchDir]] file...");
          System.exit(1);
        }
      if (filterScratch && !text) {
        System.err.println("Can't filter scratch in binary input files");
        System.exit(2);
      }
      for (int i=0; i < args.length; i++)
        System.out.println(Digest.toString(get(args[i],text,filterScratch,new File(scratchDir))));
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

}
