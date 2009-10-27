package org.dacapo.harness;

import java.io.File;

import org.dacapo.harness.Benchmark;
import org.dacapo.parser.Config;

public class Fop extends Benchmark {

  private String[] args;

  public Fop(Config config, File scratch) throws Exception {
    super(config, scratch);
    Class<?> clazz = Class.forName("org.apache.fop.cli.Main", true, loader);
    this.method = clazz.getMethod("startFOP", new Class[] { String[].class} );
  }

  @Override
  protected void prepare(String size) throws Exception {
    super.prepare(size);
    args = config.preprocessArgs(size,scratch);
    /* Retarget input/output files into scratch directory */
    for (int i=0; i < args.length; i++)
      if (args[i].charAt(0) != '-')
        args[i] = fileInScratch(args[i]);
  }

  public void iterate(String size) throws Exception {
    method.invoke(null, new Object[] {args});
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
    (new Fop(null, null)).run(null, "");
  }
}
