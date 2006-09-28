/*
 * (C) Copyright Department of Computer Science,
 * Australian National University. 2006
 */

package dacapo;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

/**
 * Attempt to match a string character by character.  
 * 
 * @author Robin Garner
 * @date $Date:$
 * @id $Id:$
 *
 */
class Matcher {
  
  private static final boolean trace = false;
  
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
   * This is the string we are looking for
   */
  private final char[] tagBuf;
  
  /**
   * The state of the matcher is the current character in the buffer that we have
   * matched to date.
   */
  private int state = 0;
  
  private final String replacement;
  
  /**
   * Create a matcher for the given string.
   * 
   * @param tag
   */
  public Matcher(String tag, String replacement) {
    tagBuf = tag.toCharArray();
    outBuf = new char[Math.max(tag.length(),replacement.length())];
    this.replacement = replacement;
  }
  
  /**
   * Has the target string been matched ?
   * 
   * @return
   */
  boolean matched() { return state == tagBuf.length; }
  
  /**
   * Is there a partial match in progress ?
   * 
   * @return
   */
  boolean matching() { return state != 0; }
  
  /**
   * Reset the state of the match
   */
  void reset() { state  = 0; }
  
  private static final int NOMATCH = 1;
  private static final int MATCHING = 2;
  private static final int MATCHED = 3;
  
  private int st = NOMATCH;
  
  public void match(char c) {
    if (trace) err.println("Matcher.match("+c+")");
    outCount = 0;
    if (tagBuf[state] == c) {
      state++;
      st = MATCHING;
    } else {
      st = NOMATCH;
    }
    if (st == MATCHING && state == tagBuf.length) {
      if (trace) err.println("Matcher.match: matched");
      st = MATCHED;
      outCount = replacement.length();
      for (int i=0; i < outCount; i++)
        outBuf[i] = replacement.charAt(i);
      state = 0;
    } else if (state > 0 && st == MATCHING) {
      if (trace) err.println("Matcher.match: matching ...");
      // Do nothing - match in progress
    } else if (state == 0 && st == NOMATCH) {
      if (trace) err.println("Matcher.match: no match on first character");
      // Failed to match first character in tag
      outCount = 1;
      outBuf[0] = c;
    } else { // st == NOMATCH
      if (trace) err.println("Matcher.match: match failed at "+state);
      System.arraycopy(tagBuf,0,outBuf,0,tagBuf.length);
      // Partial match that failed.
      int i=1;  // Emit 1 or more characters
      state--;
      boolean matched = false;
      while (state > 0 && !matched) {
        // Search forward to the start of a match
        while (tagBuf[i] != tagBuf[0] && state > 0) {
          i++;
          state--;
        }
        if (trace) err.println("Matcher.match: looking for match at "+i+","+state);
        // Match the current tag buffer agains the new output stream prefix
        matched = true;
        for (int j=0; j < state && matched; j++) {
          if (tagBuf[j] != tagBuf[i+j])
            matched = false;
        }
        
        // And match the current character
        matched = matched && (tagBuf[state] == c);
        
        
        if (matched)
          state++;  // Record match of the current char
      }
      if (!matched)
        outBuf[i++] = c;
      outCount = i;
    }
    if (trace) {
      err.print("Matcher.match - emitting \"");
      for (int i=0; i < outCount; i++) {
        err.print(outBuf[i]);
      }
      err.println("\"");
    }
  }
  
  public void flush() {
    for (int i=0; i < state; i++) {
      outBuf[i] = tagBuf[i];
    }
    outCount = state;
    state = 0;
  }
  
  private int outCount = 0;
  public char outBuf[];
  
  public int emit() {
    // Number of characters of outbuf to emit.
    return outCount;
  }
}
