/*
 * (C) Copyright Department of Computer Science,
 * Australian National University. 2005
 */

import org.dacapo.harness.Callback;
import org.dacapo.harness.CommandLineArgs;

public class MMTkCallback extends Callback {
  
  private final MMTkHarness harness = new MMTkHarness();
  
  public MMTkCallback(CommandLineArgs args) {
    super(args);
  }
  /* Immediately prior to start of the benchmark */
  public void start(String benchmark) {
    if (!isWarmup())
      harness.harnessBegin();
    super.start(benchmark);
  };
  /* Immediately after the end of the benchmark */
  public void stop() {
    super.stop();
    if (!isWarmup())
      harness.harnessEnd();
  }
}
