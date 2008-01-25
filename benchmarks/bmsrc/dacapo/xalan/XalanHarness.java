package dacapo.xalan;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;


import dacapo.Benchmark;
import dacapo.parser.Config;

/*
 * Xalan test harness. Uses a single pre-compiled stylesheet to transfrom
 * a number of sample files using a number of threads. The goal is to 
 * simulate a typical server XSLT load which is performing XML to (X)HTML
 * transforms as part of a presentation layer.
 */
public class XalanHarness extends Benchmark {
  Object benchmark;
  Method createWorkersMethod;
  
  public XalanHarness(Config config, File scratch) throws Exception {
    super(config, scratch);
    Class<?> clazz = Class.forName("dacapo.xalan.benchmark.XalanBenchmark", true, loader);
    this.method = clazz.getMethod("doWork",int.class);
    createWorkersMethod = clazz.getMethod("createWorkers", int.class);
    Constructor<?> constructor = clazz.getConstructor(File.class);
    useBenchmarkClassLoader();
    benchmark = constructor.newInstance(scratch);
  }
  
  /**
   * Create the threads, this is outside the timing loop to minimise the impact
   * of the startup. The threads will just sit waiting on the work queue.
   * 
   * @see dacapo.Benchmark#preIteration(java.lang.String)
   */
  public void preIteration(String size) throws Exception {
    super.preIteration(size);

    useBenchmarkClassLoader();
    try {
      createWorkersMethod.invoke(benchmark,new Object[] {getThreadCount(config,size)});
    } finally {
      revertClassLoader();
    }
  }


  /*
   * Run the benchmark by just pushing jobs onto the
   * work queue and waiting for the threads to finish.
   * @see dacapo.Benchmark#iterate(java.lang.String)
   */
  public void iterate(String size) throws Exception {
    String[] harnessArgs = config.getArgs(size);
    int nRuns = Integer.parseInt(harnessArgs[0]);

    method.invoke(benchmark,new Object[] {nRuns});
    System.out.println("Normal completion.");
  }





}
