package org.dacapo.harness;

import java.io.File;

import org.dacapo.harness.Benchmark;
import org.dacapo.parser.Config;

public class Bloat extends Benchmark {

  public Bloat(Config config, File scratch) throws Exception {
    super(config, scratch);
    Class<?> clazz = Class.forName("EDU.purdue.cs.bloat.optimize.Main", true, loader);
    this.method = clazz.getMethod("main", String[].class);
   }
  
  public void prepare() throws Exception {
    // do nothing
  }

  public void cleanup(){
    // do nothing
  }

  public void iterate(String size) throws Exception {
    method.invoke(null, (Object) preprocessArgs(size));
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
    (new Bloat(null, null)).run(null, "");
  }
}
