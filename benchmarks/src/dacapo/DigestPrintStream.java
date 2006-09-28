/*
 * (C) Copyright Department of Computer Science,
 * Australian National University. 2006
 */

package dacapo;

import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;
import java.security.DigestOutputStream;

/**
 * 
 * @author Robin Garner
 * @date $Date:$
 * @id $Id:$
 *
 */
public class DigestPrintStream extends PrintStream {
  
  private DigestOutputStream stream;
  
  /**
   * The real constructor.
   *
   * Note the order the two SubstitutingOutputStreams are wrapped in - 
   * it is important to substitute the absolute path before the relative path.
   */
  private DigestPrintStream(DigestOutputStream out, File scratch) {
    super(new SubstitutingOutputStream(
        new SubstitutingOutputStream(
            out,scratch.getPath(),"$SCRATCH"),
          scratch.getAbsolutePath(),"$SCRATCH"));
    stream = out;
  }
  
  /**
   * @param out
   */
  public DigestPrintStream(OutputStream out, File scratch) {
    this(new DigestOutputStream(out,Digest.create()),scratch);
  }

  /**
   * Reset the message digest
   */
  public void reset() {
    
  }
  
  public byte[] digest() {
    return stream.getMessageDigest().digest();
  }

  /* (non-Javadoc)
   * @see java.io.PrintStream#println()
   */
  public void println() {
    stream.on(false);
    super.println();
    stream.on(true);
    stream.getMessageDigest().update((byte)'\n');
  }

  /* (non-Javadoc)
   * @see java.io.PrintStream#println(boolean)
   */
  public void println(boolean x) {
    super.print(x);
    println();
  }

  /* (non-Javadoc)
   * @see java.io.PrintStream#println(char)
   */
  public void println(char x) {
    super.print(x);
    println();
  }

  /* (non-Javadoc)
   * @see java.io.PrintStream#println(char[])
   */
  public void println(char[] x) {
    super.print(x);
    println();
  }

  /* (non-Javadoc)
   * @see java.io.PrintStream#println(double)
   */
  public void println(double x) {
    super.print(x);
    println();
  }

  /* (non-Javadoc)
   * @see java.io.PrintStream#println(float)
   */
  public void println(float x) {
    super.print(x);
    println();
  }

  /* (non-Javadoc)
   * @see java.io.PrintStream#println(int)
   */
  public void println(int x) {
    super.print(x);
    println();
  }

  /* (non-Javadoc)
   * @see java.io.PrintStream#println(long)
   */
  public void println(long x) {
    super.print(x);
    println();
  }

  /* (non-Javadoc)
   * @see java.io.PrintStream#println(java.lang.Object)
   */
  public void println(Object x) {
    super.print(x);
    println();
  }

  /* (non-Javadoc)
   * @see java.io.PrintStream#println(java.lang.String)
   */
  public void println(String x) {
    super.print(x);
    println();
  }

  
}
