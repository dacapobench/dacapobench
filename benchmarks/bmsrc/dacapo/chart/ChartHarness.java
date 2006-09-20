package dacapo.chart;

import java.io.File;
import java.util.Vector;

import dacapo.Benchmark;
import dacapo.parser.Config;


public class ChartHarness extends Benchmark {

  public ChartHarness(Config config, File scratch) throws Exception {
    super(config, scratch);
  }

  public void iterate(String size) throws Exception {
    String[] args = config.getArgs(size);
    Vector newArgs = new Vector(args.length+2);
    newArgs.add("-d");
    newArgs.add(scratch.getPath());
    for (int i=0; i < args.length; i++) {
      if (args[i].equals("-p")) {
        newArgs.add(args[i++]);
        newArgs.add(args[i]);
      } else  if (args[i].equals("small") || args[i].equals("large")) {
          newArgs.add(args[i]);
      } else if (args[i].charAt(0) == '-')
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
    Plotter.main(newArgStrings);
  }

//  public void postIteration(String size) {
//    super.postIteration(size);
//    String[] args = config.getArgs(size);
//    for (int i=0; i < args.length; i++) {
//      if (args[i].equals("-p")) {
//        deleteFiles(scratch,args[++i]+".*\\.pdf");
//      } 
//    }
//  }
}
