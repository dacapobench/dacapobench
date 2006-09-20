package dacapo.jython;

import java.io.File;

import org.python.util.jython;
import org.python.core.Py;

import dacapo.Benchmark;
import dacapo.parser.Config;


public class JythonHarness extends Benchmark {
  
  public JythonHarness(Config config, File scratch) throws Exception {
    super(config, scratch);
  }
  
  protected void prepare() throws Exception {
    super.prepare();
    System.setProperty("python.home",fileInScratch("jython"));
    System.setProperty("python.cachedir",fileInScratch("cachedir"));
    jython.main(new String[] {fileInScratch("jython/noop.py")} );
  }

  /**
   * jython.main doesn't expect to be called multiple times, so we've hacked
   * Py.setArgv to allow us to reset the command line arguments that the python
   * script sees.  Hence the Py.setArgv call, followed by the jython.main call.
   */
  public void iterate(String size) throws Exception {
    String[] args = config.getArgs(size);
    args[0] = new String(fileInScratch(args[0]));
    String pyargs[] = new String[args.length - 1];
    for (int i = 0; i < pyargs.length; i++) {
      pyargs[i] = args[i+1];
    }
    Py.setArgv(args[0], pyargs);
    jython.main(args);
  }

  public void cleanup() {
    super.cleanup();
    deleteTree(new File(scratch,"cachedir"));
  }
}
