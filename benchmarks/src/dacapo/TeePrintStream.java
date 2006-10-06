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
   * @param arg0
   */
  public TeePrintStream(OutputStream arg0, File logFile) {
    super(arg0);
  }
}
