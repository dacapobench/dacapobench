package org.dacapo.harness;

import java.io.File;

import org.dacapo.harness.Benchmark;
import org.dacapo.parser.Config;

public class Hsqldb extends Benchmark {

  public Hsqldb(Config config, File scratch) throws Exception {
    super(config, scratch);
    Class<?> clazz = Class.forName("org.dacapo.jdbcbench.PseudoJDBCBench", true, loader);
    this.method = clazz.getMethod("main",String[].class);
  }
  
  protected void prepare() {
    // Do nothing
  }

  public void iterate(String size) throws Exception {
    method.invoke(null, (Object)preprocessArgs(size));
  }
  
  public void postIteration(String size) throws Exception {
    deleteFile(new File(scratch,"hsqldb.properties"));
    deleteFile(new File(scratch,"hsqldb.script"));
    super.postIteration(size);
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
    (new Hsqldb(null, null)).run(null, "");
  }
}
