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
}
