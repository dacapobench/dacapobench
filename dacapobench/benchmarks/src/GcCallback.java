/*
 * (C) Copyright Department of Computer Science,
 * Australian National University. 2005
 */
import dacapo.Callback;

public class GcCallback extends Callback {
  /* perform a System.gc() before starting each run */
  public void start(String benchmark) {
    System.gc();
    super.start(benchmark);
  };
  public void startWarmup(String benchmark) {
    System.gc();
    super.startWarmup(benchmark);
  };
}
