/*
 * (C) Copyright Department of Computer Science,
 * Australian National University. 2006
 */

package dacapo;

import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;

/**
 * 
 * @author Robin Garner
 * @date $Date:$
 * @id $Id:$
 *
 */
public class TeePrintStream extends PrintStream {
  
  /**
   * @param dest The destination stream (around which this class wraps)
   * @param logFile Log a copy to this file.
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
  
  public void openLog() {
    ((TeeOutputStream)out).openLog();
  }
  public void closeLog() {
    ((TeeOutputStream)out).closeLog();
  }
}
