/*
 * (C) Copyright Department of Computer Science,
 * Australian National University. 2005
 */
import dacapo.Callback;
import dacapo.CommandLineArgs;

public class GcCallback extends Callback {
  
  public GcCallback(CommandLineArgs args) {
    super(args);
  }
  /* perform a System.gc() before starting each run */
  public void start(String benchmark) {
    System.gc();
    super.start(benchmark);
  };
  public void startWarmup(String benchmark) {
    System.gc();
    super.start(benchmark);
  };
}
