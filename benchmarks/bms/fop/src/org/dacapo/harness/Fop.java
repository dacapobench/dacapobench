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
    args = preprocessArgs(size);
    /* Retarget input/output files into scratch directory */
    for (int i=0; i < args.length; i++)
      if (args[i].charAt(0) != '-')
        args[i] = fileInScratch(args[i]);
  }

  public void iterate(String size) throws Exception {
    method.invoke(null, new Object[] {args});
  }
}
