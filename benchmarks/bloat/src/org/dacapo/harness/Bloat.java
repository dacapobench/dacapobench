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
}
