package dacapo.derby;

import java.io.File;

import dacapo.Benchmark;
import dacapo.PseudoJDBCBench;
import dacapo.parser.Config;

public class DerbyHarness extends Benchmark {

  public DerbyHarness(Config config, File scratch) throws Exception {
    super(config, scratch);
  }
  
  @Override
  protected void prepare() throws Exception {
    /* Don't call super.prepare - we don't have a data zip file to unpack */
  }

  
  /**
   * The pre-iteration 
   */
  @Override
  public void prepare(String size) throws Exception {
    System.out.println("Populating the database");

    /* Modify the benchmark args to set -tpc to 0, and add "-init" */
    String[] args = config.getArgs(size);
    for (int i=0; i < args.length; i++) {
      if (args[i].equals("-tpc")) {
        args[++i] = "0";
      }
    }
    String[] initArgs = new String[args.length+1];
    System.arraycopy(args, 0, initArgs, 0, args.length);
    initArgs[initArgs.length-1] = "-init";
    PseudoJDBCBench.main(substitute("$SCRATCH",scratch.getAbsolutePath(),initArgs));
  }

  @Override
  public void iterate(String size) throws Exception {
    PseudoJDBCBench.main(substitute("$SCRATCH",scratch.getAbsolutePath(),config.getArgs(size)));
  }
  
  @Override
  public void postIteration(String size) throws Exception {
    super.postIteration(size);
  }
  
  private String[] substitute(String pattern, String subst, String[] args) {
    String[] result = new String[args.length];
    for (int i=0; i < args.length; i++) {
      result[i] = args[i].replace(pattern, subst);
    }
    return result;
  }
}
