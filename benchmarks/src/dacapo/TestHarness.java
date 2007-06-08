/*
 * 
 */
package dacapo;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.text.DecimalFormat;

import dacapo.parser.Config;

/**
 * Main class for the Dacapo benchmark suite.  Locates the configuration file 
 * for the specified benchmark, interprets command line arguments, and invokes 
 * the benchmark-specific harness class.
 * 
 * @author Steve Blackburn
 * @author Robin Garner
 *
 */
public class TestHarness {
  private final Config config;
  private static CommandLineArgs commandLineArgs;
  
  public static final DecimalFormat two_dp = twoDecimalPlaces();
  
  private static URL getURL(String fn) {
    ClassLoader cl = TestHarness.class.getClassLoader();
    if (commandLineArgs.isVerbose())
      System.out.println("TestHarness.getURL: returns "+cl.getResource(fn));
    return cl.getResource(fn);    
  }
  
  public static boolean exists(File f) {
    return exists(f.getPath());
  }
  
  public static boolean exists(String fn) {
    boolean result = getURL(fn) != null;
    if (!result && commandLineArgs.allowOpenFromFileSystem()) {
      if (commandLineArgs.isVerbose())
        System.out.println("TestHarness.exists: going to file system for "+fn);
      File file = new File(fn);
      result =  file.exists();
    }
    return result;
  }
  
  
  
  /**
   * Calculates coefficient of variation of a set of longs (standard deviation
   * divided by mean).
   * 
   * @param times Array of input values
   * @return Coefficient of variation
   */
  public static double coeff_of_var(long[] times) {
    double n = times.length;
    double sum = 0.0;
    double sum2 = 0.0;
    
    for (int i=0; i < times.length; i++) {
      double x = times[i];
      sum += x;
      sum2 += x * x;
    }
    
    double mean = sum / n;
    double sigma = Math.sqrt(1.0/n * sum2 - mean * mean);
    
    return sigma/mean;
  }
  
  public static void main(String[] args) {
    try {
      commandLineArgs = new CommandLineArgs(args);
      
      File scratch = new File(commandLineArgs.getScratchDir());
      rmdir(scratch);
      scratch.mkdir();
      
      // now get the benchmark names and run them
      for (String bm : commandLineArgs.benchmarks()) {
        // check if it is a benchmark name
        // name of file containing configurations
        String cnf = "cnf/"+bm+".cnf";
        InputStream ins = TestHarness.class.getClassLoader().getResourceAsStream(cnf);
        if (ins == null) {
          System.err.println("Unknown benchmark: "+bm);
          System.exit(20);
        }
        
        TestHarness harness = new TestHarness(ins);
        
        if (commandLineArgs.isInfo()) {
          harness.bmInfo();
        } else {
          if (commandLineArgs.isVerbose())
            harness.dump();
          
          runBenchmark(scratch, bm, harness);
        }
      }
    }
    catch (Exception e) {
      System.err.println(e);
      e.printStackTrace();
      System.exit(-1);
    }
  }

  /**
   * @param scratch
   * @param bm
   * @param harness
   * @param c
   * @throws NoSuchMethodException
   * @throws InstantiationException
   * @throws IllegalAccessException
   * @throws InvocationTargetException
   * @throws Exception
   */
  private static void runBenchmark(File scratch, String bm, TestHarness harness) throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException, Exception {
    Constructor<?> cons = harness.findClass().getConstructor(new Class[] {Config.class,File.class});
    
    Benchmark b = (Benchmark) cons.newInstance(new Object[] {harness.config,scratch});
    
    boolean valid = true;
    Callback callback = commandLineArgs.getCallback();
    callback.init(harness.config);
    
    do {
      valid = b.run(callback, commandLineArgs.getSize(), !callback.isWarmup()) && valid;
    } while (callback.runAgain());
    b.cleanup();
    
    if (!valid) {
      System.err.println("Validation FAILED for "+bm+" "+commandLineArgs.getSize());
      if (!commandLineArgs.isIgnoreValidation())
        System.exit(-2);
    }
  }

  /**
   * @return A Decimal Format object
   */
  private static DecimalFormat twoDecimalPlaces() {
    DecimalFormat two_dp;
    two_dp = new DecimalFormat();
    two_dp.setMaximumFractionDigits(2);
    two_dp.setMinimumFractionDigits(2);
    two_dp.setGroupingUsed(true);
    return two_dp;
  }

  private static void rmdir(File dir) {
    String[] files = dir.list();
    if (files != null) {
      for (int f = 0; f < files.length; f++) {
        File file = new File(dir, files[f]);
        if (file.isDirectory())
          rmdir(file);
        if (!file.delete())
          System.err.println("Could not delete "+files[f]);
      }
    }
  }
  
  public static int TEST(int i) {
    System.err.println("In TEST");
    System.err.println(i);
    return 2*i;
  }
  
  private void bmInfo() {
    config.describe(System.err);
  }
  
  private void dump() {
    System.err.println("Class name: "+config.className);
    System.err.println("Method name: "+config.methodName);
    
    System.err.println("Configurations:");
    config.describe(System.err);
  }
  
  private TestHarness(InputStream stream) {
    config = Config.parse(stream);
    if (config == null)
      System.exit(-1);
  }
  
  private Class<?> findClass() {
    try {
      return Class.forName(config.className);
    }
    catch (ClassNotFoundException e) {
      System.err.println(e);
      e.printStackTrace();
      System.exit(-1);
      return null;  // not reached
    }
  }
}
