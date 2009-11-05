package org.dacapo.harness;

import java.io.File;
import java.lang.reflect.Method;

import org.dacapo.harness.Benchmark;
import org.dacapo.parser.Config;

/**
 * Dacapo benchmark harness for tradebeans.
 * 
 * @author Apache
 *
 */

public class Tradebeans extends Benchmark {
  
  public final String TRADEBEANS_LOG_FILE_NAME = "tradebeans.log";
  
  private Method initializeMethod;
  
  public Tradebeans(Config config, File scratch) throws Exception {
    super(config,scratch,false);

    // frankly this seems like a hack to get around a wierdness with J9
    // it appears that J9 might do something odd with the class initialization
    // order which causes the log file not to be found.  So we force the creation
    // here.
    File log = new File(scratch,TRADEBEANS_LOG_FILE_NAME).getAbsoluteFile();
    
    // create the containing directory if necessary
    log.getParentFile().mkdirs();
    // create the log file (empty) if necessary
    log.createNewFile();
    
    System.setProperty("java.util.logging.config.file", log.toString());
    
    // Find the launcher
    Class<?> clazz = Class.forName("org.dacapo.daytrader.Launcher", true, loader);
    this.initializeMethod = clazz.getMethod("initialize", new Class[] { File.class, Integer.TYPE, String.class, Boolean.TYPE } );
    this.method = clazz.getMethod("performIteration", new Class[] { } );
  }
  
  @Override
  protected void prepare() throws Exception {
    unpackZipFileResource("dat/daytrader.zip", scratch);
  }
  
  @Override
  protected void prepare(String size) throws Exception {
    String[] args = config.preprocessArgs(size,scratch);
    String dtSize = "medium";
    if (args.length == 1)
      dtSize = args[0];
    
    initializeMethod.invoke(null, scratch, config.getThreadCount(size), dtSize, true);
  }
  
  public void cleanup() {
    System.out.println("Shutting down Geronimo...");
    if (!isPreserve()) {
      deleteTree(new File(scratch,"tradebeans"));
      deleteTree(new File(scratch,"geronimo-jetty6-minimal-2.1.4"));
    }
  }
  
  public void iterate(String size) throws Exception {
    if (isVerbose())
      System.out.println("tradebeans benchmark starting");
    method.invoke(null);
  }
  
  /**
   * Stub which exists <b>only</b> to facilitate whole program
   * static analysis on a per-benchmark basis.  See also the "split-deps"
   * ant build target, which is also provided to enable whole program
   * static analysis.
   * 
   * @author Eric Bodden
   */
  public static void main(String args[]) throws Exception {
    // create dummy harness and invoke with dummy arguments
    (new Tradebeans(null, null)).run(null, "");
  }
}
