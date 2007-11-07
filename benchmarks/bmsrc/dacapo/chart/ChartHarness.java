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
    Plotter.main(preprocessArgs(size));
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
