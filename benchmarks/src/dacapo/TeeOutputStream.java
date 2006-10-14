/*
 * (C) Copyright Department of Computer Science,
 * Australian National University. 2006
 */

package dacapo;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * 
 * @author Robin Garner
 * @date $Date:$
 * @id $Id:$
 *
 */
public class TeeOutputStream extends FilterOutputStream {

  private OutputStream log = null;
  private int version = 0;
  private final File logFile;
  
  /**
   * 
   */
  public TeeOutputStream(OutputStream stream, File logFile) {
    super(stream);
    this.logFile = logFile;
    newLog();
  }

  /**
   * Open the logfile.
   * 
   * @param logFile
   */
  private void newLog() {
    try {
      log = new FileOutputStream(logFile);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
  }
  
  public void openLog() {
    newLog();
  }
  
  public void closeLog() {
    try {
      flush();
      log.close();
      log = null;
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
  
  /* (non-Javadoc)
   * @see java.io.FilterOutputStream#close()
   */
  public void close() throws IOException {
    super.close();
    if (log != null) log.close();
  }
  /* (non-Javadoc)
   * @see java.io.FilterOutputStream#flush()
   */
  public void flush() throws IOException {
    super.flush();
    if (log != null) log.flush();
  }
  /* (non-Javadoc)
   * @see java.io.FilterOutputStream#write(int)
   */
  public void write(int b) throws IOException {
    super.write(b);
    if (log != null) log.write(b);
  }

  /* (non-Javadoc)
   * @see java.lang.Object#finalize()
   */
  protected void finalize() throws Throwable {
    try { 
      flush();
      close();
      super.finalize();
    } catch (Exception e) {}
  }

  public void version() {
    version++;
    File archive = new File(logFile.getAbsolutePath()+"."+version);
    if (log != null) 
      try { log.close(); } catch (IOException e) {}
    logFile.renameTo(archive);
  }
}
