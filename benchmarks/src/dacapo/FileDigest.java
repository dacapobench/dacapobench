package dacapo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.DigestInputStream;
import java.security.MessageDigest;

public class FileDigest {
  
  
  
  public FileDigest() {
    
  }
  
  public static byte[] get(String file) throws IOException { return get(new File(file)); }
  public static byte[] get(File dir, String file) throws IOException { return get(new File(dir,file)); }
  
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
