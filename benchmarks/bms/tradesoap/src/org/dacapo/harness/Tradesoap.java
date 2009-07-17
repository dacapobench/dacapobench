package org.dacapo.harness;

import java.io.File;
import java.lang.reflect.Method;

import org.dacapo.harness.Benchmark;
import org.dacapo.parser.Config;

/**
 * Dacapo benchmark harness for tradesoap.
 * 
 * @author Apache
 *
 */

public class Tradesoap extends Benchmark {
  private Method initializeMethod;
    
  public Tradesoap(Config config, File scratch) throws Exception {
    super(config,scratch,false);
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
    String[] args = preprocessArgs(size);
    String dtSize = "medium";
    if (args.length == 1)
      dtSize = args[0];
    
    initializeMethod.invoke(null, scratch, getThreadCount(config,size), dtSize, false);
  }
  
  public void cleanup() {
    System.out.println("Shutting down Geronimo...");
    if (!isPreserve()) {
      deleteTree(new File(scratch,"tradesoap"));
      deleteTree(new File(scratch,"geronimo-jetty6-minimal-2.1.4"));
    }
  }
  
  public void iterate(String size) throws Exception {
    if (isVerbose())
      System.out.println("tradesoap benchmark starting");
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
    (new Tradesoap(null, null)).run(null, "");
  }
}
