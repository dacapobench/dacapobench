package dacapo.fop;

import java.io.File;
import java.lang.reflect.Method;

import dacapo.Benchmark;
import dacapo.parser.Config;

public class FopHarness extends Benchmark {

  private Method method;

  public FopHarness(Config config, File scratch) throws Exception {
    super(config, scratch);
    Class clazz = Class.forName("org.apache.fop.cli.Main", true, loader);
    this.method = clazz.getMethod("startFOP", new Class[] { String[].class} );
  }

  public void iterate(String size) throws Exception {
    String[] args = preprocessArgs(size);
    /* Retarget input/output files into scratch directory */
    for (int i=0; i < args.length; i++)
      if (args[i].charAt(0) != '-')
        args[i] = fileInScratch(args[i]);

    ClassLoader dacapoCL = Thread.currentThread().getContextClassLoader();
    Thread.currentThread().setContextClassLoader(loader);
    method.invoke(null, new Object[] {args});
    Thread.currentThread().setContextClassLoader(dacapoCL);
  }
}
