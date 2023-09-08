/*
 * 
 */
package dacapo;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.Vector;

import dacapo.parser.Config;

/**
 * Main class for the Dacapo benchmark suite.  Locates the configuration file 
 * for the specified benchmark, interprets command line arguments, and invokes 
 * the benchmark-specific harness class.
 * 
 * $Id: TestHarness.java 260 2007-01-25 05:54:17Z steveb-oss $
 * $Date: 2007-01-25 16:54:17 +1100 (Thu, 25 Jan 2007) $
 * 
 * @author Steve Blackburn
 * @author Robin Garner
 *
 */
public class TestHarness {
  private final Config config;
  
  private static boolean verbose = false;
  
  private static boolean allowOpenFromFileSystem = false;

  private static boolean converge = false;

  private static double target_var = 3.0/100;

  private static int window = 3;

  private static int max_iterations = 20;

  private static boolean ignoreValidation = false;

  private static int iterations = 1;

  private static String size = "default";

  private static String scratchDir = "./scratch";

  private static Callback callback = null;

  private static boolean info = false;

  private static final DecimalFormat two_dp = twoDecimalPlaces();
  
  private static URL getURL(String fn) {
    ClassLoader cl = TestHarness.class.getClassLoader();
    if (verbose)
      System.out.println("TestHarness.getURL: returns "+cl.getResource(fn));
    return cl.getResource(fn);    
  }
  
  public static boolean exists(File f) {
    return exists(f.getPath());
  }
  
  public static boolean exists(String fn) {
    boolean result = getURL(fn) != null;
    if (!result && allowOpenFromFileSystem) {
      if (verbose)
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
  private static double coeff_of_var(long[] times) {
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
      /* No options - print usage and die */
      if (args.length == 0) {
        printUsage();
        return;
      }
      
      /* get global options */
      int i = 0;
      for (; i < args.length && args[i].charAt(0) == '-'; i++) {
    	try {
        if (args[i].equals("-s")) {
          // size name name
          if (i == args.length - 1) {
            System.err.println("No size specified! (\"-h\" for usage)");
            System.exit(10);    	  
          }
          size = args[++i];
        } else if (args[i].equals("-i")) {
          // display benchmark information
          info = true;
        } else if (args[i].equals("-h")) {
          printUsage();
        } else if (args[i].equals("-v")) {
          // display detailed information
          verbose = true;
        } else if (args[i].equals("-c")) {
          // use a callback
          if (i == args.length - 1) {
            System.err.println("No callback class specified! (\"-h\" for usage)");
            System.exit(11);    	  
          }
          Class cls = null;
          try {
            cls = Class.forName(args[++i]);
          } catch (Exception e) {
            System.err.println(e);
            System.err.println("Could not find callback class "+args[i]);
            System.exit(12);
          } 
          if (!(Class.forName("dacapo.Callback").isAssignableFrom(cls))) {
            System.err.println(args[i] + " is not an instance of dacapo.Callback");
            System.exit(13);
          } else {
            callback = (Callback) cls.newInstance();
          }
        } else if (args[i].equals("-n")) {          // Run n times, showing the last iteration
          if (i == args.length - 1) {
            System.err.println("Number of iterations not specified! (\"-h\" for usage)");
            System.exit(14);    	  
          }
          iterations = Integer.parseInt(args[++i]);
        } else if (args[i].equals("-converge")) {   // Run until times converge
          converge = true;
        } else if (args[i].equals("-max_iterations")) { // Max iterations for convergence
          if (i == args.length - 1) {
            System.err.println("No max specified! (\"-h\" for usage)");
            System.exit(15);    	  
          }
          max_iterations = Integer.parseInt(args[++i]);
        } else if (args[i].equals("-variance")) {   // Coeff. of variance to aim for
          if (i == args.length - 1) {
            System.err.println("No variance specified! (\"-h\" for usage)");
            System.exit(16);    	  
          }
          target_var = Double.parseDouble(args[++i])/100.0;
        } else if (args[i].equals("-window")) {     // # iterations to average convergence over
          if (i == args.length - 1) {
            System.err.println("No window size specified!  (\"-h\" for usage)");
            System.exit(17);    	  
          }
          window = Integer.parseInt(args[++i]);
        } else if (args[i].equals("-debug")) {      // Verbose benchmark output
          Benchmark.setVerbose(true);
        } else if (args[i].equals("-preserve")) {   // Preserve scratch directory contents
          Benchmark.setPreserve(true);
        } else if (args[i].equals("-noDigestOutput")) {  // 
          Benchmark.setValidateOutput(false);
        } else if (args[i].equals("-ignoreValidation")) {
          ignoreValidation = true;
        } else if (args[i].equals("-noValidation")) {
          Benchmark.setValidate(false);
        } else if (args[i].equals("-validationReport")) {
          Benchmark.enableValidationReport(args[++i]);
        } else if (args[i].equals("-scratch")) {
          if (i == args.length - 1) {
            System.err.println("No scratch directory specified! (\"-h\" for usage)");
            System.exit(18);    	  
          }
          scratchDir = args[++i];
        } else {
          System.err.println("Unrecognized option "+args[i]+ " (\"-h\" for usage)");
          System.exit(1);
        }
    	} catch (NumberFormatException e) {
          System.err.println("Could not parse numeric argument to \""+args[i-1]+"\"! (\"-h\" for usage)");
          System.exit(18);    	  
    	}
      }
      
      if (i == args.length) {
          System.err.println("No benchmarks specified! (\"-h\" for usage)");
          System.exit(19);    	  
      }
      
      if (callback == null) {
        callback = new Callback();
      }
      
      File scratch = new File(scratchDir);
      rmdir(scratch);
      scratch.mkdir();
      
      // now get the benchmark names and run them
      for (; i < args.length; i++) {
        // check if it is a benchmark name
        // name of file containing configurations
        String bm = args[i];
        String cnf = "cnf/"+bm+".cnf";
        InputStream ins = TestHarness.class.getClassLoader().getResourceAsStream(cnf);
        if (ins == null) {
          System.err.println("Unknown benchmark: "+args[i]);
          System.exit(20);
        }
        
        TestHarness harness = new TestHarness(ins);
        
        if (info) {
          harness.bmInfo();
        } else {
          if (verbose)
            harness.dump();
          
          if (dacapo.Benchmark.class.isAssignableFrom(harness.findClass())) {
            runBenchmark(scratch, bm, harness);
          } else {
            runBenchmarkOld(bm, harness);
          }
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
    Constructor cons = harness.findClass().getConstructor(new Class[] {Config.class,File.class});
    
    Benchmark b = (Benchmark) cons.newInstance(new Object[] {harness.config,scratch});
    
    boolean valid = true;
    if (converge) {
      /*
       * Run the benchmark using convergence 
       */
      long[] times = new long[window];
      int n = 0;
      
      /* Warmup */
      while (n < window || (n < max_iterations && coeff_of_var(times) > target_var) ) {
        long start_time = System.currentTimeMillis();
        valid = b.run(callback, size, false) && valid;
        times[n%window] = System.currentTimeMillis() - start_time;
        n++;
        if (n >= window && verbose) {
          System.err.println("Variation "+two_dp.format(coeff_of_var(times)*100)+
                  "% achieved after "+n+" iterations");
        }
      }
      if (n < max_iterations) {
        valid = b.run(callback, size, true) && valid; // beware order of evaluation!
      } else {
        System.err.println("Benchmark failed to converge.");
      }
    } else {
      /*
       * Run the benchmark for a set # of iterations
       */
      for (int n = 0; n < iterations - 1; n++)
        valid = b.run(callback, size, false) && valid; // beware order of evaluation!
      valid = b.run(callback, size, true) && valid; // beware order of evaluation!
    }            
    b.cleanup();
    
    if (!valid) {
      System.err.println("Validation FAILED for "+bm+" "+size);
      if (!ignoreValidation)
        System.exit(-2);
    }
  }

  /**
   * @param bm
   * @param harness
   */
  private static void runBenchmarkOld(String bm, TestHarness harness) {
    /*
     * Old-style benchmarks
     */
    Method m = harness.findMethod();

    for (; iterations > 1; iterations--) {
      callback.startWarmup(bm);
      harness.invokeConfiguration(m, size);
      callback.stopWarmup();
      callback.completeWarmup(bm, true);
    }

    callback.start(bm);
    harness.invokeConfiguration(m, size);
    callback.stop();
    callback.complete(bm, true);
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

  /**
   * Print a usage message to stdout
   */
  private static void printUsage() {
    System.out.println("Usage: java -jar dacapo-<version>.jar [options ...] [benchmarks ...]");
    System.out.println("    -c <callback>           Use class <callback> to bracket benchmark runs");
    System.out.println("    -h                      Print this help");
    System.out.println("    -i                      Display benchmark information");
    System.out.println("    -s small|default|large  Size of input data");
    System.out.println();
    System.out.println("  Measurement methodology options");
    System.out.println("    -converge               Allow benchmark times to converge before timing");
    System.out.println("      -max_iterations <n>     Run a max of n iterations (default 20)");
    System.out.println("      -variance <pct>         Target coefficient of variation (default 3.0)");
    System.out.println("      -window <n>             Measure variance over n runs (default 3)");
    System.out.println("    -n <iter>               Run the benchmark <iter> times");
    System.out.println();
    System.out.println("  Debugging options (for benchmark suite maintainers)");
    System.out.println("    -debug                  Verbose debugging information");
    System.out.println("    -ignoreValidation       Don't halt on validation failure");
    System.out.println("    -noDigestOutput         Turn off SHA1 digest of stdout/stderr");
    System.out.println("    -noValidation           Don't validate at all");
    System.out.println("    -preserve               Preserve output files (debug)");
    System.out.println("    -v                      Verbose output");
    System.out.println("    -validationReport       Report digests, line counts etc");
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
  
  private Class findClass() {
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
  
  private Method findMethod() {
    return findMethod(config.methodName);
  }
  
  private Method findMethod(String methodName) {
    Method[] methods = findClass().getDeclaredMethods();
    for (int i=0; i < methods.length; i++) {
      Method m = methods[i];
      if (m.getName().equals(methodName)) {
        return m;
      }
    }

    System.err.println("Method not found: "+methodName);
    System.exit(-1);
    return null;
  }
  
  private static Vector vectorise(String[] s) {
    Vector result = new Vector(s.length);
    for (int i=0; i < s.length; i++)
      result.add(s[i]);
    return result;
  }
  
  private void invokeConfiguration(Method m, String configuration) {
    try {
      Vector configArgs = vectorise(config.getArgs(configuration));
      if (configArgs == null) {
        System.err.println("Can't find configuration "+ configuration);
        System.exit(-1);
      }
      
      Class[] parameters = m.getParameterTypes();
      Object[] invocationArgs = new Object[parameters.length];
      
      // Special case to handle Spec benchmarks, 'main' programs,
      // etc, which take a single argument that is an array of strings
      if (parameters.length == 1 &&
              parameters[0].isArray() &&
              parameters[0].getComponentType() == "".getClass()) {
        
        String[] sArray = new String[configArgs.size()];
        for (int i = 0; i < configArgs.size(); i++)
          sArray[i] = (String)configArgs.elementAt(i);
        invocationArgs[0] = sArray;
      }
      else {
        for (int i = 0; i < parameters.length; i++) {
          Class ptype = parameters[i];
          Object arg = configArgs.elementAt(i);
          if (ptype.isInstance(arg)) {
            invocationArgs[i] = arg;
          }
          else if (ptype == Boolean.TYPE) {
            invocationArgs[i] = new Boolean((String)arg);
          }
          else if (ptype == Byte.TYPE) {
            invocationArgs[i] = new Byte(((Double)arg).byteValue());
          }
          else if (ptype == Short.TYPE) {
            invocationArgs[i] = new Short(((Double)arg).shortValue());
          }
          else if (ptype == Integer.TYPE) {
            invocationArgs[i] = new Integer(((Double)arg).intValue());
          }
          else if (ptype == Long.TYPE) {
            invocationArgs[i] = new Long(((Double)arg).longValue());
          }
          else if (ptype == Float.TYPE) {
            invocationArgs[i] = new Float(((Double)arg).floatValue());
          }
          else if (ptype == Double.TYPE) {
            invocationArgs[i] = new  
            Double(((Double)arg).doubleValue());
          }
        }
      }
      
      Object instance = null;
      
      if (verbose) {
        System.out.println("method is: "+m);
      }
      
      
      if (!Modifier.isStatic(m.getModifiers()))
        instance = m.getDeclaringClass().newInstance();
      
      Object result = m.invoke(instance, invocationArgs);
      
      if (result != null) {
        System.out.println("Invocation result was: "+result);
      }
    }
    catch (Exception e) {
      System.err.println(e);
//      e.printStackTrace();
      System.exit(-1);
    }
  }
}
