/*
 * (C) Copyright Department of Computer Science,
 * Australian National University. 2006
 */

package dacapo;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

/**
 * Output stream that looks for a character sequence and replaces it with a substitute.
 * 
 * @author Robin Garner
 * @date $Date:$
 * @id $Id:$
 *
 */
public class SubstitutingOutputStream extends FilterOutputStream {
  
  private static PrintStream err;
  static {
    try {
      err = new PrintStream(new FileOutputStream("/dev/stdout"));
    } catch (FileNotFoundException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
  
  /**
   * The strings we're looking for, along with the dynamic match state
   */
  private final Matcher match;
  
  /**
   * @param out
   */
  public SubstitutingOutputStream(OutputStream out, String lookFor, String replaceWith) {
    super(out);
//    err.println("Replacing \""+lookFor+"\" with \""+replaceWith+"\"");
    match = new Matcher(lookFor,replaceWith);
  }

  /* (non-Javadoc)
   * @see java.io.FilterOutputStream#write(int)
   */
  public void write(int c) throws IOException {
    match.match((char)c);
    for (int i=0; i < match.emit(); i++)
      super.write(match.outBuf[i]);
  }
  
  public void close() throws IOException {
    flush();
    super.close();
  }

  public void flush() throws IOException {
    match.flush();
    for (int i=0; i < match.emit(); i++)
      super.write(match.outBuf[i]);
    super.flush();
  }
  
  /**
   * Unit test code
   * 
   * @param args
   */
  public static void main(String[] args) {
    PrintStream newOut = new PrintStream(new SubstitutingOutputStream(
        System.out,"fred","jim"));
    test(newOut,"hi there","hi there");
    test(newOut,"fred", "jim");
    test(newOut,"alfreddy", "'aljimdy'");
    test(newOut,"ffrfrefred", "'ffrfrejim'");
    
    newOut = new PrintStream(new SubstitutingOutputStream(
        System.out,"xxy","***"));
    
    test(newOut, "x", "x");
    test(newOut, "xx","xx");
    test(newOut, "xy", "xy");
    test(newOut, "xxy", "***");
    test(newOut, "xxxy", "x***");
    test(newOut, "xxxxy", "xx***");
    test(newOut, "xxxxxy", "xxx***");
    test(newOut, "xxyxxy", "******");
    test(newOut, "xxxyxy", "x***xy");
    
    newOut = new PrintStream(new SubstitutingOutputStream(
        System.out,"/tmp/dacapo","$SCRATCH"));
    test(newOut, "/tmp", "/tmp");
    test(newOut, "./scratch/dacapo", "./scratch/dacapo");
    test(newOut, "/tmp/dacapo", "$SCRATCH");
    test(newOut, "/tmp/tmp/dacapo", "/tmp$SCRATCH");
    test(newOut, "/tmp/dacapo/tmp/dacapo", "$SCRATCH$SCRATCH");
    
    newOut = new PrintStream(
        new SubstitutingOutputStream(
            new SubstitutingOutputStream(
                System.out,"./scratch","$SCRATCH"),
            "/home/robing/ws_dacapo/dacapo/./scratch","$SCRATCH"));
    
    test(newOut, "adding luindex/william/histories/kinghenryv", 
        "adding luindex/william/histories/kinghenryv");
  }

  private static void test(PrintStream newOut, String inString, String outString) {
    err.println("Input string "+inString);
    newOut.print(inString); 
    newOut.flush(); 
    err.flush();
    System.out.println(" <- should read "+outString);
    System.out.flush();
  }
}
