package dacapo.fop;

import java.io.File;

import dacapo.Benchmark;
import dacapo.parser.Config;

public class FopHarness extends Benchmark {

  public FopHarness(Config config, File scratch) throws Exception {
    super(config, scratch);
    getBenchmarkMethod();
  }

  public void iterate(String size) throws Exception {
    String[] args = preprocessArgs(size);
    /* Retarget input/output files into scratch directory */
    for (int i=0; i < args.length; i++)
      if (args[i].charAt(0) != '-')
        args[i] = fileInScratch(args[i]);

    invoke(args);
  }
}
