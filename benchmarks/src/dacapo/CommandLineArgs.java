/**
 * 
 */
package dacapo;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

/**
 * Command line arguments for a dacapo benchmark run.
 * 
 * Encapsulated in an object so that it can be passed to user-written
 * callbacks.
 * 
 * @author Robin Garner
 *
 */
public class CommandLineArgs {
  
  
  public enum Methodology { ITERATE, CONVERGE; }

  private boolean verbose = false;
  private boolean allowOpenFromFileSystem = false;
  private Methodology methodology = Methodology.ITERATE;
  private double targetVar = 3.0/100;
  private int window = 3;
  private int maxIterations = 20;
  private boolean ignoreValidation = false;
  private int iterations = 1;
  private String size = "default";
  private String scratchDir = "./scratch";
  private Callback callback = null;
  private boolean info = false;
  
  private List<String> benchmarks = new ArrayList<String>();
  
  CommandLineArgs(String[] args) throws Exception {
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
        this.size = args[++i];
      } else if (args[i].equals("-i")) {
        // display benchmark information
        this.info = true;
      } else if (args[i].equals("-h")) {
        printUsage();
      } else if (args[i].equals("-v")) {
        // display detailed information
        this.verbose = true;
      } else if (args[i].equals("-c")) {
        // use a callback
        if (i == args.length - 1) {
          System.err.println("No callback class specified! (\"-h\" for usage)");
          System.exit(11);          
        }
        Class<?> cls = null;
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
          Constructor<?> cons = cls.getConstructor(new Class[] { CommandLineArgs.class });
          this.callback = ((Callback) cons.newInstance(new Object[] {this}));
        }
      } else if (args[i].equals("-n")) {          // Run n times, showing the last iteration
        if (i == args.length - 1) {
          System.err.println("Number of iterations not specified! (\"-h\" for usage)");
          System.exit(14);          
        }
        this.iterations = Integer.parseInt(args[++i]);
      } else if (args[i].equals("-converge")) {   // Run until times converge
        this.methodology = Methodology.CONVERGE;
      } else if (args[i].equals("-max_iterations")) { // Max iterations for convergence
        if (i == args.length - 1) {
          System.err.println("No max specified! (\"-h\" for usage)");
          System.exit(15);          
        }
        this.maxIterations = Integer.parseInt(args[++i]);
      } else if (args[i].equals("-variance")) {   // Coeff. of variance to aim for
        if (i == args.length - 1) {
          System.err.println("No variance specified! (\"-h\" for usage)");
          System.exit(16);          
        }
        this.targetVar = Double.parseDouble(args[++i])/100.0;
      } else if (args[i].equals("-window")) {     // # iterations to average convergence over
        if (i == args.length - 1) {
          System.err.println("No window size specified!  (\"-h\" for usage)");
          System.exit(17);          
        }
        this.window = Integer.parseInt(args[++i]);
      } else if (args[i].equals("-debug")) {      // Verbose benchmark output
        Benchmark.setVerbose(true);
      } else if (args[i].equals("-preserve")) {   // Preserve scratch directory contents
        Benchmark.setPreserve(true);
      } else if (args[i].equals("-noDigestOutput")) {  // 
        Benchmark.setValidateOutput(false);
      } else if (args[i].equals("-ignoreValidation")) {
        this.ignoreValidation = true;
      } else if (args[i].equals("-noValidation")) {
        Benchmark.setValidate(false);
      } else if (args[i].equals("-validationReport")) {
        Benchmark.enableValidationReport(args[++i]);
      } else if (args[i].equals("-scratch")) {
        if (i == args.length - 1) {
          System.err.println("No scratch directory specified! (\"-h\" for usage)");
          System.exit(18);          
        }
        this.scratchDir = args[++i];
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
    
    // now get the benchmark names
    for (; i < args.length; i++) {
      String bm = args[i];
      if (bm.charAt(0) == '-') {
        System.err.println(args[i]+ " is not a benchmark name (\"-h\" for usage)");
        System.exit(1);
      }
      
      benchmarks.add(bm);
    }
    
    /* Add the default callback */
    if (getCallback() == null) {
      callback = new Callback(this);
    }
  }

  /**
   * Print a usage message to stdout
   */
  static void printUsage() {
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
  

  /*
   * Getter methods
   */
  
  public boolean isVerbose() {
    return verbose;
  }
  public boolean allowOpenFromFileSystem() {
    return allowOpenFromFileSystem;
  }
  public Methodology methodology() {
    return methodology;
  }
  public boolean convergeMethodology() {
    return methodology == Methodology.CONVERGE;
  }
  public boolean iterateMethodology() {
    return methodology == Methodology.ITERATE;
  }
  public double getTargetVar() {
    return targetVar;
  }
  public int getWindow() {
    return window;
  }
  public int getMaxIterations() {
    return maxIterations;
  }
  public boolean isIgnoreValidation() {
    return ignoreValidation;
  }
  public int getIterations() {
    return iterations;
  }
  public String getSize() {
    return size;
  }
  public String getScratchDir() {
    return scratchDir;
  }
  public Callback getCallback() {
    return callback;
  }
  public boolean isInfo() {
    return info;
  }
  public Iterable<String> benchmarks() {
    return benchmarks;
  }
}
