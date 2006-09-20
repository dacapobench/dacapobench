package dacapo.batik;

import java.io.File;
import java.util.Vector;

import dacapo.Benchmark;
import dacapo.parser.Config;

public class BatikHarness extends Benchmark {

  public BatikHarness(Config config, File scratch) throws Exception {
    super(config, scratch);
  }

  
  /**
   * Args is a list of file names relative to the scratch directory
   */
  public void iterate(String size) throws Exception {
    String[] args = config.getArgs(size);
    Vector newArgs = new Vector(args.length+2);
    newArgs.add("-d");
    newArgs.add(scratch.getPath());
    for (int i=0; i < args.length; i++) {
      if (args[i].charAt(0) == '-')
        newArgs.add(args[i]);
      else
        newArgs.add((new File(scratch,args[i])).getPath());
    }
    String[] newArgStrings = (String[])newArgs.toArray(new String[0]);
    if (verbose) {
      for (int i=0; i < newArgStrings.length; i++) 
        System.out.print(newArgStrings[i]+" ");
      System.out.println();
    }
    org.apache.batik.apps.rasterizer.Main.main(newArgStrings);
  }
}
