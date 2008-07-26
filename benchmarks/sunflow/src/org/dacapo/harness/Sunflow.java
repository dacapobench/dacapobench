/*
 * This class is directly derived from org.sunflow.Benchmark
 *  
 *  Copyright (c) 2003-2007 Christopher Kulla
 *  
 *  Permission is hereby granted, free of charge, to any person obtaining a copy of
 *  this software and associated documentation files (the "Software"), to deal in the
 *  Software without restriction, including without limitation the rights to use, copy,
 *  modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
 *  and to permit persons to whom the Software is furnished to do so, subject to the
 *  following conditions:
 *  
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *  
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 *  INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 *  PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 *  HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 *  OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 *  SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.dacapo.harness;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import org.dacapo.parser.Config;


public class Sunflow extends org.dacapo.harness.Benchmark {
  
  private final Constructor<?> constructor;
  private Object sunflow;
  private final Method beginMethod;
  private final Method endMethod;
  
  public Sunflow(Config config, File scratch) throws Exception {
    super(config, scratch);
    Class<?> clazz = Class.forName("org.sunflow.Benchmark", true, loader);
    this.method = clazz.getMethod("kernelMain");
    this.beginMethod = clazz.getMethod("kernelBegin");
    this.endMethod = clazz.getMethod("kernelEnd");
    this.constructor = clazz.getConstructor(
        int.class,boolean.class,boolean.class,boolean.class);
  }

  /** Do one-time prep such as unziping data.  In our case, do nothing. */
  protected void prepare() {}

  /**
   * Code to execute prior to each iteration OUTSIDE the timing loop.  In this case we
   * create a new instance of a Sunflow benchmark, which sets up basic data structures.
   * 
   * @param size The "size" of the iteration (small, default, large)
   */
  public void preIteration(String size) throws Exception {
    String[] args = preprocessArgs(size);
    useBenchmarkClassLoader();
    try {
      sunflow = constructor.newInstance(Integer.parseInt(args[0]), false, false, false);
      beginMethod.invoke(sunflow);
    } finally {
      revertClassLoader();
    }
  }
    
  /**
   * Perform a single iteration of the benchmark.
   * 
   * @param size The "size" of the iteration (small, default, large)
   */
  public void iterate(String size) throws Exception {
    method.invoke(sunflow);
  }
  
  /**
   * Validate the output of the benchmark, OUTSIDE the timing loop.
   * 
   * @param size The "size" of the iteration (small, default, large)
   */
  public boolean validate(String size) {
    if (!validate)
      return true;
    try {
      useBenchmarkClassLoader();
      try {
        endMethod.invoke(sunflow);
      } finally {
        revertClassLoader();
      }
    } catch (RuntimeException e) {
      System.err.println(e.getMessage());
      return false;
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
    return super.validate(size);
  }
}
