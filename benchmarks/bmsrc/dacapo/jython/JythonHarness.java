package dacapo.jython;

import java.io.File;
import java.lang.reflect.Method;

import dacapo.Benchmark;
import dacapo.parser.Config;


public class JythonHarness extends Benchmark {
  
  private Method pySetArgsMethod;
  
  public JythonHarness(Config config, File scratch) throws Exception {
    super(config, scratch);
    Class<?> clazz = Class.forName("org.python.util.jython", true, loader);
    this.method = clazz.getMethod("main", String[].class );
    Class<?> pyClass = Class.forName("org.python.core.Py", true, loader);
    pySetArgsMethod = pyClass.getMethod("setArgv", String.class, String[].class );
    System.setProperty("python.home",fileInScratch("jython"));
    System.setProperty("python.cachedir",fileInScratch("cachedir"));
    useBenchmarkClassLoader();
    try {
      method.invoke(null, (Object)new String[] {fileInScratch("jython/noop.py")} );
    } finally {
      revertClassLoader();
    }
  }
  
  /**
   * jython.main doesn't expect to be called multiple times, so we've hacked
   * Py.setArgv to allow us to reset the command line arguments that the python
   * script sees.  Hence the Py.setArgv call, followed by the jython.main call.
   */
  public void iterate(String size) throws Exception {
    String[] args = preprocessArgs(size);
    String pyargs[] = new String[args.length - 1];
    for (int i = 0; i < pyargs.length; i++) {
      pyargs[i] = args[i+1];
    }
    pySetArgsMethod.invoke(null, args[0], pyargs);
    method.invoke(null, (Object)args);
  }

  public void cleanup() {
    super.cleanup();
    deleteTree(new File(scratch,"cachedir"));
  }
}
