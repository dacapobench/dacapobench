package org.dacapo.daytrader;

import java.io.InputStream;

import org.apache.geronimo.cli.daemon.DaemonCLParser;
import org.apache.geronimo.kernel.Kernel;
import org.apache.geronimo.kernel.KernelFactory;
import org.apache.geronimo.kernel.config.ConfigurationUtil;
import org.apache.geronimo.kernel.log.GeronimoLogging;
import org.apache.geronimo.kernel.util.Main;

public class DaCapoServerRunner {
  private static Kernel kernel = null;
  private static Thread serverThread = null;
  
  public static void initialize() {
    try {
      GeronimoLogging.initialize(GeronimoLogging.ERROR);
      final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
      kernel = KernelFactory.newInstance().createKernel("DaCapoServer");
      kernel.boot();
      InputStream in = classLoader.getResourceAsStream("META-INF/config.ser");
      ConfigurationUtil.loadBootstrapConfiguration(kernel, in, classLoader, true);
      final DaemonCLParser parser = new DaemonCLParser(System.out);
      final Main main = (Main) kernel.getGBean(Main.class);
      parser.parse(new String[] { "--quiet" });
      serverThread = new Thread(new Runnable() {
        public void run() {
          Thread.currentThread().setContextClassLoader(main.getClass().getClassLoader());
          main.execute(parser);
        }
      });
      serverThread.start();
    } catch (Exception e) {
      System.err.print("Exception initializing server: " + e.toString());
      e.printStackTrace();
      System.exit(-1);
    }
  }
  
  public static void shutdown() {
    while (serverThread.isAlive()) {
      serverThread.interrupt();
      try {
        serverThread.join();
      } catch (InterruptedException e) {}
    }
    serverThread = null;
    kernel.shutdown();
    kernel = null;
  }
}
