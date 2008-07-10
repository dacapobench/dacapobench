package dacapo.bloat;

import java.io.File;

import dacapo.Benchmark;
import dacapo.parser.Config;

public class BloatHarness extends Benchmark {

  public BloatHarness(Config config, File scratch) throws Exception {
    super(config, scratch);
    Class<?> clazz = Class.forName("EDU.purdue.cs.bloat.optimize.Main", true, loader);
    this.method = clazz.getMethod("main", String[].class);
   }
  
  public void prepare() throws Exception {
    // do nothing
  }

  public void cleanup(){
    // do nothing
  }

  public void iterate(String size) throws Exception {
    method.invoke(null, (Object) preprocessArgs(size));
  }
}
