package dacapo.batik;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.Vector;

import dacapo.Benchmark;
import dacapo.parser.Config;

public class BatikHarness extends Benchmark {

  private String[] args;
  private final Constructor<?> constructor;

  public BatikHarness(Config config, File scratch) throws Exception {
    super(config, scratch);
    Class<?> clazz = Class.forName("org.apache.batik.apps.rasterizer.Main", true, loader);
    this.method = clazz.getMethod("execute");
    this.constructor = clazz.getConstructor(String[].class);
  }

  
  @Override
  protected void prepare(String size) throws Exception {
    super.prepare(size);
    String[] args = preprocessArgs(size);
    Vector<String> newArgs = new Vector<String>(args.length+2);
    for (int i=0; i < args.length; i++) {
      if (args[i].charAt(0) == '-') {
        if (args[i].equals("-m") || args[i].equals("-d")) {
          newArgs.add(args[i]);
          newArgs.add(args[++i]);
        } else 
          newArgs.add(args[i]);
      } else
        newArgs.add((new File(scratch,args[i])).getPath());
    }
    String[] newArgStrings = (String[])newArgs.toArray(new String[0]);
    if (isVerbose()) {
      for (int i=0; i < newArgStrings.length; i++) 
        System.out.print(newArgStrings[i]+" ");
      System.out.println();
    }
    this.args = newArgStrings;
  }


  /**
   * Args is a list of file names relative to the scratch directory
   */
  public void iterate(String size) throws Exception {
    Object object = constructor.newInstance((Object)args);
    method.invoke(object);
  }
}
