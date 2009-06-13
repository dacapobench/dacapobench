package org.dacapo.daytrader;

import java.io.*;

public class ServerThread extends Thread {
  private static final String SCRATCH_DIR = "./scratch";
  private static final String[] SERVER_ARGS = {"--quiet"};
  
  public void run() {
//    File scratch = new File((new File(SCRATCH_DIR)).getAbsolutePath());
    Launcher.startServer(SERVER_ARGS);
  }
}
