package dacapo;

public class Callback {
  
  protected long timer;
  
  /* Start the timer and announce the begining of an iteration */
  public void start(String benchmark) {
    start(benchmark, false);
  };
  public void startWarmup(String benchmark) {
    start(benchmark, true);
  };
  private void start(String benchmark, boolean warmup) {
    timer = System.currentTimeMillis();
    System.err.print("===== DaCapo "+ benchmark + " starting ");
    System.err.println((warmup ? "warmup " : "") + "=====");
    System.err.flush();
  }
  /* Stop the timer */
  public void stop() {
    timer = System.currentTimeMillis() - timer;
  }
  public void stopWarmup() {
    timer = System.currentTimeMillis() - timer;
  }
  /* Announce completion of the benchmark (pass or fail) */
  public void complete(String benchmark, boolean valid) {
    complete(benchmark, valid, false);
  };
  public void completeWarmup(String benchmark, boolean valid) {
    complete(benchmark, valid, true);
  };
  private void complete(String benchmark, boolean valid, boolean warmup) {
    System.err.print("===== DaCapo "+ benchmark);
    if (valid) {
      System.err.print(warmup ? " completed warmup " : " PASSED ");
      System.err.print("in " + timer + " msec ");   
    } else {
      System.err.print(" FAILED " + (warmup ? "warmup " : ""));
    }
    System.err.println("=====");
    System.err.flush();
  }
}
