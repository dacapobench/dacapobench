package dacapo.hsqldb;

import java.io.File;

import dacapo.Benchmark;
import dacapo.parser.Config;

public class HsqldbHarness extends Benchmark {

  public HsqldbHarness(Config config, File scratch) throws Exception {
    super(config, scratch);
    Class<?> clazz = Class.forName("dacapo.PseudoJDBCBench", true, loader);
    this.method = clazz.getMethod("main",String[].class);
  }
  
  protected void prepare() {
    // Do nothing
  }

  public void iterate(String size) throws Exception {
    method.invoke(null, (Object)preprocessArgs(size));
  }
  
  public void postIteration(String size) throws Exception {
    deleteFile(new File(scratch,"hsqldb.properties"));
    deleteFile(new File(scratch,"hsqldb.script"));
    super.postIteration(size);
  }
}
