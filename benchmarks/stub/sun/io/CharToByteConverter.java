/*
 * (C) Copyright Department of Computer Science,
 * Australian National University. 2006
 */

package sun.io;

import java.io.UnsupportedEncodingException;

/**
 * This class is not provided by runtimes that aren't based on the sun libraries
 * This class does the minimum required for the xalan benchmark to run.
 * 
 * @author Robin Garner
 * @date $Date:$
 * @id $Id:$
 *
 */
public class CharToByteConverter {
  private static final CharToByteConverter instance = new CharToByteConverter();
  public static CharToByteConverter getConverter(String converter) throws UnsupportedEncodingException {
    return instance;
  }
  public boolean canConvert(char c) {
    return true;
  }
}
