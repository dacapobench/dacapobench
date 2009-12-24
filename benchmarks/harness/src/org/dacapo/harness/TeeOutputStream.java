/*
 * Copyright (c) 2006, 2009 The Australian National University.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License v2.0.
 * You may obtain the license at
 * 
 *    http://www.opensource.org/licenses/apache2.0.php
 */
package org.dacapo.harness;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * @date $Date: 2009-12-24 11:19:36 +1100 (Thu, 24 Dec 2009) $
 * @id $Id: TeeOutputStream.java 738 2009-12-24 00:19:36Z steveb-oss $
 */
public class TeeOutputStream extends FilterOutputStream {

  private OutputStream log = null;
  private int version = 0;
  private final File logFile;
  private boolean toScreen = false;

  /**
   * Constructor. Output is sent to both the output stream and the log file.
   */
  public TeeOutputStream(OutputStream stream, File logFile) {
    super(stream);
    this.logFile = logFile;
    newLog();
  }

  /**
   * Let output through to the screen
   */
  public void enableOutput(boolean enable) {
    toScreen = enable;
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

  /*
   * (non-Javadoc)
   * 
   * @see java.io.FilterOutputStream#close()
   */
  public void close() throws IOException {
    super.close();
    if (log != null)
      log.close();
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.io.FilterOutputStream#flush()
   */
  public void flush() throws IOException {
    super.flush();
    if (log != null)
      log.flush();
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.io.FilterOutputStream#write(int)
   */
  public void write(int b) throws IOException {
    if (toScreen) {
      super.write(b);
      super.flush();
    }
    if (log != null) {
      log.write(b);
      log.flush();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#finalize()
   */
  protected void finalize() throws Throwable {
    try {
      flush();
      close();
      super.finalize();
    } catch (Exception e) {
    }
  }

  public void version() {
    version++;
    File archive = new File(logFile.getAbsolutePath() + "." + version);
    if (log != null)
      try {
        log.close();
      } catch (IOException e) {
      }
    logFile.renameTo(archive);
  }
}
