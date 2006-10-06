/*
 * (C) Copyright Department of Computer Science,
 * Australian National University. 2006
 */

package dacapo;

import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

/**
 * 
 * @author Robin Garner
 * @date $Date:$
 * @id $Id:$
 *
 */
public class TeePrintStream extends PrintStream {
  
  /**
   * @param dest
   */
  public TeePrintStream(OutputStream dest, File logFile) {
    super(new TeeOutputStream(dest,logFile));
  }
  
  /**
   * Start a new log file, creating an archived version of the current one.
   *
   */
  public void version() {
    ((TeeOutputStream)out).version();
  }
}
