package dacapo.fop;

import java.io.File;
import org.apache.fop.apps.Fop;

import dacapo.Benchmark;
import dacapo.parser.Config;

public class FopHarness extends Benchmark {

  public FopHarness(Config config, File scratch) throws Exception {
    super(config, scratch);
  }

  public void iterate(String size) throws Exception {
    String[] args = config.getArgs(size);
    /* Retarget input/output files into scratch directory */
    for (int i=0; i < args.length; i++) 
      if (args[i].charAt(0) != '-')
        args[i] = fileInScratch(args[i]);
    Fop.main(args);
  }

}
