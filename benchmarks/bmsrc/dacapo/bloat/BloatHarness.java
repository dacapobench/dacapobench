package dacapo.bloat;

import java.io.File;

import dacapo.Benchmark;
import dacapo.parser.Config;

public class BloatHarness extends Benchmark {

  public BloatHarness(Config config, File scratch) throws Exception {
    super(config, scratch);
  }
  
  public void prepare() throws Exception {
    // do nothing
  }

  public void cleanup(){
    // do nothing
  }

  public void iterate(String size) throws Exception {
    String[] args = config.getArgs(size);
    args[args.length-1] = fileInScratch(args[args.length-1]);
    EDU.purdue.cs.bloat.optimize.Main.main(args);
  }

}
