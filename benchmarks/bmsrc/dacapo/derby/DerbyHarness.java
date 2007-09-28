package dacapo.derby;

import java.io.File;

import dacapo.Benchmark;
import dacapo.PseudoJDBCBench;
import dacapo.parser.Config;

public class DerbyHarness extends Benchmark {

  public DerbyHarness(Config config, File scratch) throws Exception {
    super(config, scratch);
  }
  
  protected void prepare() {
    // Do nothing
  }

  public void iterate(String size) throws Exception {
    PseudoJDBCBench.main(substitute("$SCRATCH",scratch.getAbsolutePath(),config.getArgs(size)));
  }
  
  public void postIteration(String size) {
    deleteFile(new File(scratch,"hsqldb.properties"));
    deleteFile(new File(scratch,"hsqldb.script"));
  }
  
  private String[] substitute(String pattern, String subst, String[] args) {
    String[] result = new String[args.length];
    for (int i=0; i < args.length; i++) {
      result[i] = args[i].replace(pattern, subst);
    }
    return result;
  }
}
