package org.dacapo.harness;

import java.io.File;

import org.dacapo.harness.Benchmark;
import org.dacapo.parser.Config;

public class Derby extends Benchmark {

  public Derby(Config config, File scratch) throws Exception {
    super(config, scratch);
    Class<?> clazz = Class.forName("org.dacapo.jdbcbench.PseudoJDBCBench", true, loader);
    this.method = clazz.getMethod("main",String[].class);
  }
  
  @Override
  protected void prepare() throws Exception {
    /* Don't call super.prepare - we don't have a data zip file to unpack */
  }

  
  /**
   * The pre-iteration 
   */
  @Override
  public void prepare(String size) throws Exception {
    System.out.println("Populating the database");

    /* Modify the benchmark args to set -tpc to 0, and add "-init" */
    String[] args = preprocessArgs(size);
    String[] initArgs = new String[args.length+1];
    for (int i=0; i < args.length; i++) {
      initArgs[i] = args[i];
      if (args[i].equals("-tpc") || args[i].equals("-total_trans")) {
        initArgs[++i] = "0";
      } 
    }
    initArgs[initArgs.length-1] = "-init";
    useBenchmarkClassLoader();
    try {
      method.invoke(null, (Object)initArgs);
    } finally {
      revertClassLoader();
    }
  }

  @Override
  public void iterate(String size) throws Exception {
    method.invoke(null, (Object)preprocessArgs(size));
  }
  
  @Override
  public void postIteration(String size) throws Exception {
    super.postIteration(size);
  }
}
