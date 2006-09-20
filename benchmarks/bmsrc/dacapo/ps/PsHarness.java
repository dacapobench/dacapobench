package dacapo.ps;

import java.io.File;

import dacapo.Benchmark;
import dacapo.parser.Config;

import edu.unm.cs.oal.DaCapo.JavaPostScript.Red.executive;

public class PsHarness extends Benchmark {

  public PsHarness(Config config, File scratch) throws Exception {
    super(config, scratch);
  }

  public void iterate(String size) throws Exception {
    String[] args = config.getArgs(size);
    /* Retarget input/output files into scratch directory */
    for (int i=0; i < args.length; i++) 
      if (args[i].charAt(0) != '-')
        args[i] = fileInScratch(args[i]);
    executive.main(args);
  }

}
