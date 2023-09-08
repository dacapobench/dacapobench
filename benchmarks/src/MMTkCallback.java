/*
 * (C) Copyright Department of Computer Science,
 * Australian National University. 2005
 */

import dacapo.Callback;

public class MMTkCallback extends Callback {
  
  private final MMTkHarness harness = new MMTkHarness();
  
  /* Immediatly prior to start of the benchmark */
  public void start(String benchmark) {
    harness.harnessBegin();
    super.start(benchmark);
  };
  /* Immediatly after the end of the benchmark */
  public void stop() {
    super.stop();
    harness.harnessEnd();
  }
}
