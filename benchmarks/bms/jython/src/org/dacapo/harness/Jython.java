package org.dacapo.harness;

import java.io.File;
import java.lang.reflect.Method;

import org.dacapo.harness.Benchmark;
import org.dacapo.parser.Config;

public class Jython extends Benchmark {

  private Method pySetArgsMethod;

  public Jython(Config config, File scratch) throws Exception {
    super(config, scratch);
    Class<?> clazz = Class.forName("org.python.util.jython", true, loader);
    this.method = clazz.getMethod("main", String[].class);
    Class<?> pyClass = Class.forName("org.python.core.PySystemState", true,
        loader);
    pySetArgsMethod = pyClass.getMethod("setArgv", String[].class);
    System.setProperty("python.home", fileInScratch("jython"));
    System.setProperty("python.cachedir", fileInScratch("cachedir"));
    System.setProperty("python.verbose", "warning");
    useBenchmarkClassLoader();
    try {
      method.invoke(null,
          (Object) new String[] { fileInScratch("jython/noop.py") });
    } finally {
      revertClassLoader();
    }
  }

  /**
   * jython.main doesn't expect to be called multiple times, so we've hacked
   * Py.setArgv to allow us to reset the command line arguments that the python
   * script sees. Hence the Py.setArgv call, followed by the jython.main call.
   */
  public void iterate(String size) throws Exception {
    String[] args = config.preprocessArgs(size, scratch);
    pySetArgsMethod.invoke(null, (Object) args);
    method.invoke(null, (Object) args);
  }

  public void cleanup() {
    super.cleanup();
    deleteTree(new File(scratch, "cachedir"));
  }

  /**
   * Stub which exists <b>only</b> to facilitate whole program static analysis
   * on a per-benchmark basis. See also the "split-deps" ant build target, which
   * is also provided to enable whole program static analysis.
   * 
   * @author Eric Bodden
   */
  public static void main(String args[]) throws Exception {
    // create dummy harness and invoke with dummy arguments
    (new Jython(null, null)).run(null, "");
  }
}
