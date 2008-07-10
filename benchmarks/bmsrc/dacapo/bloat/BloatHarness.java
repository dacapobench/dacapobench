package dacapo.bloat;

import java.io.File;
import java.lang.reflect.Constructor;

import dacapo.Benchmark;
import dacapo.parser.Config;

public class BloatHarness extends Benchmark {

  private final Constructor<?> constructor;

  public BloatHarness(Config config, File scratch) throws Exception {
    super(config, scratch);
    Class<?> clazz = Class.forName("EDU.purdue.cs.bloat.optimize.Main", true, loader);
    this.method = clazz.getMethod("main");
    this.constructor = clazz.getConstructor(String[].class);
   }
  
  public void prepare() throws Exception {
    // do nothing
  }

  public void cleanup(){
    // do nothing
  }

  public void iterate(String size) throws Exception {
    String[] args = preprocessArgs(size);
    Object object = constructor.newInstance((Object)args);
    method.invoke(object);
   }

}
