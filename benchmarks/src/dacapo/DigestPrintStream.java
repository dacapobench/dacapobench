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
 * Version of a PrintStream class that canonicalises its pinput in several ways, and
 * computes a hash of the  stream.
 * - Newlines are hashed in a platform-independent way
 * - The relative and absolute versions of the scratch directory path are
 *   replaced with '$SCRATCH'
 * 
 * @author Robin Garner
 * @date $Date:$
 * @id $Id:$
 *
 */
public class DigestPrintStream extends PrintStream {
  
  private final TeeOutputStream teeStream;
  private final DigestOutputStream digestStream;
  
  /**
   * The real constructor.
   *
   * Note the order the two SubstitutingOutputStreams are wrapped in - 
   * it is important to substitute the absolute path before the relative path.
   */
  private DigestPrintStream(
      DigestOutputStream digestStream, 
      TeeOutputStream teeStream, 
      File scratch, 
      String logFile) {
    super(new SubstitutingOutputStream(
        new SubstitutingOutputStream(
            teeStream,
            scratch.getPath(),"$SCRATCH"),
          scratch.getAbsolutePath(),"$SCRATCH"));
    this.teeStream = teeStream;
    this.digestStream = digestStream;
  }

  /**
   * @param out
   */
  public static DigestPrintStream create(OutputStream out, File scratch, String logFile) {
    DigestOutputStream digestStream = new DigestOutputStream(out,Digest.create());
    TeeOutputStream teeStream = new TeeOutputStream(digestStream,new File(scratch,logFile));
    return new DigestPrintStream(digestStream,teeStream,scratch,logFile);
  }

  /**
   * Reset the message digest
   */
  public void reset() {
    digestStream.getMessageDigest().reset();
  }
  
  /**
   * Read and reset the message digest
   * @return
   */
  public byte[] digest() {
    return digestStream.getMessageDigest().digest();
  }
  
  /**
   * Start a new log file, archiving the existing one.
   */
  public void version() {
    teeStream.version();
  }

  /* (non-Javadoc)
   * @see java.io.PrintStream#println()
   */
  public void println() {
    digestStream.on(false);
    super.println();
    digestStream.on(true);
    digestStream.getMessageDigest().update((byte)'\n');
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
