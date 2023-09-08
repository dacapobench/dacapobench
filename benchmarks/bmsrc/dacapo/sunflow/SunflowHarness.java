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
package dacapo.sunflow;

import java.io.File;
import dacapo.parser.Config;
import org.sunflow.Benchmark;


public class SunflowHarness extends dacapo.Benchmark {

  private Benchmark sunflow;
  
  public SunflowHarness(Config config, File scratch) throws Exception {
    super(config, scratch);
  }

  /** Do one-time prep such as unziping data.  In our case, do nothing. */
  protected void prepare() {}

  /**
   * Code to execute prior to each iteration OUTSIDE the timing loop.  In this case we
   * create a new instance of a Sunflow benchmark, which sets up basic data structures.
   * 
   * @param size The "size" of the iteration (small, default, large)
   */
  public void preIteration(String size) {
    String[] args = config.getArgs(size);
    sunflow = new Benchmark(Integer.parseInt(args[0]), false, false, false);
    sunflow.kernelBegin();
  }
    
  /**
   * Perform a single iteration of the benchmark.
   * 
   * @param size The "size" of the iteration (small, default, large)
   */
  public void iterate(String size) {
    sunflow.kernelMain();
  }
  
  /**
   * Validate the output of the benchmark, OUTSIDE the timing loop.
   * 
   * @param size The "size" of the iteration (small, default, large)
   */
  public boolean validate(String size) {
    sunflow.kernelEnd();     
    return super.validate(size);
  }
}
