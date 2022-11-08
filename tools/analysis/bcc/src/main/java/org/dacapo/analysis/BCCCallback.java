package org.dacapo.analysis;

import org.dacapo.harness.Callback;
import org.dacapo.harness.CommandLineArgs;

public class BCCCallback extends Callback {
  public BCCCallback(CommandLineArgs args) {
    super(args);
  }

  /* Immediately prior to start of the benchmark */
  @Override
  public void start(String benchmark) {
    BCCAnalysis.iterationStart(isWarmup());
    super.start(benchmark);
  };

  /* Immediately after the end of the benchmark */
  @Override
  public void stop(long duration) {
    super.stop(duration);
    BCCAnalysis.iterationStop(isWarmup());
  };

  @Override
  public void complete(String benchmark, boolean valid) {
    super.complete(benchmark, valid);
    BCCAnalysis.benchmarkComplete(valid);
  };
}
