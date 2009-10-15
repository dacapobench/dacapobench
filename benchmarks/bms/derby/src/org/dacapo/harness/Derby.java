package org.dacapo.harness;

import java.io.File;
import java.lang.reflect.Method;

import org.dacapo.harness.Benchmark;
import org.dacapo.parser.Config;

public class Derby extends Benchmark {

  // 
  private Object tpcc;
  private Method makeTPCC;
  private Method prepareTPCC;
  private Method preIterationTPCC;
  private Method iterationTPCC;
  private Method postIterationTPCC;
  
  public Derby(Config config, File scratch) throws Exception {
    super(config, scratch);
  }
  
  @Override
  protected void prepare() throws Exception {
    useBenchmarkClassLoader();
    /* Don't call super.prepare - we don't have a data zip file to unpack */
    try {
      Class<?> tpccClazz  = Class.forName("org.dacapo.derby.TPCC",true,loader);
      this.makeTPCC = tpccClazz.getMethod("make", Config.class, File.class);
      this.prepareTPCC = tpccClazz.getMethod("prepare", String.class);
      this.preIterationTPCC = tpccClazz.getMethod("preIteration", String.class);
      this.iterationTPCC = tpccClazz.getMethod("iteration", String.class);
      this.postIterationTPCC = tpccClazz.getMethod("postIteration", String.class);

      // construct the benchmark
      this.tpcc = this.makeTPCC.invoke(null, config, scratch);
    } finally {
      revertClassLoader();
    }
  }

  
  /**
   * The benchmark run 
   */
  @Override
  public void prepare(String size) throws Exception {
    System.out.println("Populating the database");

    useBenchmarkClassLoader();
    try {
      this.prepareTPCC.invoke(this.tpcc, size);
    } finally {
      revertClassLoader();
    }
  }

  @Override
  public void preIteration(String size) throws Exception {
    useBenchmarkClassLoader();
    try {
      this.preIterationTPCC.invoke(this.tpcc, size);
    } finally {
      revertClassLoader();
    }
  }
  
  @Override
  public void iterate(String size) throws Exception {
    useBenchmarkClassLoader();
    try {
      this.iterationTPCC.invoke(this.tpcc, size);
    } finally {
      revertClassLoader();
    }
  }
  
  @Override
  public void postIteration(String size) throws Exception {
    useBenchmarkClassLoader();
    try {
      this.postIterationTPCC.invoke(this.tpcc, size);
    } finally {
      revertClassLoader();
      super.postIteration(size);
    }
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
    (new Derby(null, null)).run(null, "");
  }
}
