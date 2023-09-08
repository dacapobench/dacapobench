/*
  The following contains comments on how to use this Callback class
  and how to build the Callback class.

  USE:

    The DelayCallback class below adds a delay before and after the execution of
    the timed (final) iteration of a benchmark.  The time delay does not affect the 
    measured time for the execution of the timed iteration.

    The default delay is 10 seconnds.
 
    Two properties can be set:
    1) benchmark.sleep.time  specified the number of seconds that the main thread
       sleeps before and after the timed iteration.  If it is unspecified or the
       number is not an integer then it will default to 10 seconds.

    2) benchmark.sleep.time.verbose  when set to anything will a message before
       and after the sleep time so that you know that it is working.

    Example:

    java -Dbenchmark.sleep.time=5 -Dbenchmark.sleep.time.verbose= -jar dacapo-anjana.jar -c DelayCallback -n 2 xalan

    This will run the xalan benchmark two times, one warmup and one timed iteration.  It will
    use the DelayCallback and on the timed iteration add a delay of 5 seconds before and after
    that iteration.  It will also report a message before and after the timed iteration.

    NOTE: The dacapo-anjana.jar is the full dacapo.jar with the DelayCallback.class file add to it, see below.

  BUILD:

   You will need to do the following:
   1) Get the dacapo.jar file.

   2) Compile the DelayCallback.java file as follows:
      javac -classpath dacapo.jar DelayCallback.java

   3) Make a new jar file with the DelayCallback.class file in that jar file.  The
      following sequence should work (on linux see NOTE below for windows):

      mkdir tmp
      cd tmp
      jar xf ../dacapo.jar
      cp ../DelayCallback.class .
      jar cfm ../dacapo-anjana.jar META-INF/MANIFEST.MF .
   
   NOTE: For windows use \ instead of / and copy instead of cp

 */

import org.dacapo.harness.Callback;
import org.dacapo.harness.CommandLineArgs;
import org.dacapo.parser.Config;

public class DelayCallback extends Callback {
  
  // on the command line set the benchmark sleep time setting the java property
  //   benchmark.sleep.time
  // for example:
  //   java -Dbenchmark.sleep.time=5 -jar dacapo.jar -c DelayCallback -n 2 xalan
  public static final String BENCHMARK_SLEEP_TIME = "benchmark.sleep.time";
  public static final String BENCHMARK_SLEEP_TIME_VERBOSE = "benchmark.sleep.time.verbose";
  
  // default sleep time
  static int sleepTime = 10;
  static boolean sleepTimeVerbose = false;

  static {
    // turn on reporting if the BENCHMARK_SLEEP_TIME_VERBOSE property is set
    sleepTimeVerbose = System.getProperty(BENCHMARK_SLEEP_TIME_VERBOSE)!=null;

    try {
      // try and parse the BENCHMARK_SLEEP_TIME proprety, if it 
      // does not exist or fails to parse then default to 10 seconds
      sleepTime = Integer.parseInt(System.getProperty(BENCHMARK_SLEEP_TIME));
      if (sleepTimeVerbose)
        System.err.println("Delay benchmark.sleep.time="+sleepTime);
    } catch (Exception e) { }
  }

  public DelayCallback(CommandLineArgs args) {
    super(args);
  }

  @Override
  protected void start(String benchmark, boolean warmup) {
    if (!warmup) {
      if (sleepTimeVerbose)
        System.err.println("DelayCallback.start");
      try {
        Thread.currentThread().sleep(sleepTime*1000);
      } catch (Exception e) {
        // just consume the exception and continue on
      }
    }
    super.start(benchmark,warmup);
  }

  @Override
  public void stop(long duration, boolean warmup) {
    super.stop(duration, warmup);
    if (!warmup) {
      try {
        Thread.currentThread().sleep(sleepTime*1000);
      } catch (Exception e) {
        // just consume the exception and continue on
      }
      if (sleepTimeVerbose)
        System.err.println("DelayCallback.stop");
    }
  }
}
