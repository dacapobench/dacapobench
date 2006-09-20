package dacapo.hsqldb;

import java.io.File;

import dacapo.Benchmark;
import dacapo.parser.Config;

public class HsqldbHarness extends Benchmark {

  public HsqldbHarness(Config config, File scratch) throws Exception {
    super(config, scratch);
  }
  
  protected void prepare() {
    // Do nothing
  }

  public void iterate(String size) throws Exception {
    PseudoJDBCBench.main(config.getArgs(size));
  }
  
  public void postIteration(String size) {
    deleteFile(new File(scratch,"hsqldb.properties"));
    deleteFile(new File(scratch,"hsqldb.script"));
  }
}
