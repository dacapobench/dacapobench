/**
 * 
 */
package org.dacapo.harness;

import java.io.File;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.jar.JarEntry;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

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
  
  private final static int EXIT_OK                 = 0;
  private final static int EXIT_MISSING_CALLBACK   = 2;
  private final static int EXIT_BAD_CALLBACK       = 3;
  private final static int EXIT_BAD_COMMANDLINE    = 4;
  private final static int EXIT_MISSING_BENCHMARKS = 10;
  
  private static final String RELEASE_NOTES             = "RELEASE_NOTES.txt";
  private static final String DEFAULT_SIZE              = "default";
  private static final String DEFAULT_SCRATCH_DIRECTORY = "./scratch";
  private static final int    DEFAULT_MAX_ITERATIONS    = 20;
  private static final int    DEFAULT_WINDOW_SIZE       = 3;
  private static final double DEFAULT_VARIANCE          = 3.0/100;
  private static final int    DEFAULT_ITERATIONS        = 1;
  
  private static final String OPT_CALLBACK          = "callback";
  private static final String OPT_HELP              = "help";
  private static final String OPT_RELEASE_NOTES     = "release-notes";
  private static final String OPT_LIST_BENCHMARKS   = "list-benchmarks";
  private static final String OPT_INFORMATION       = "information";
  private static final String OPT_SIZE              = "size";
  private static final String OPT_SCRATCH_DIRECTORY = "scratch-directory";
  private static final String OPT_CONVERGE          = "converge";
  private static final String OPT_MAX_ITERATIONS    = "max-iterations";
  private static final String OPT_VARIANCE          = "variance";
  private static final String OPT_WINDOW            = "window";
  private static final String OPT_ITERATIONS        = "iterations";
  private static final String OPT_DEBUG             = "debug";
  private static final String OPT_IGNORE_VALIDATION = "ignore-validation";
  private static final String OPT_NO_DIGEST_OUTPUT  = "no-digest-output";
  private static final String OPT_NO_VALIDATION     = "no-validation";
  private static final String OPT_PRESERVE          = "preserve";
  private static final String OPT_VALIDATION_REPORT = "validation-report";
  private static final String OPT_CONFIG            = "config";
  private static final String OPT_VERBOSE           = "verbose";
  
  private static final Option[] OPTIONS = {
    makeOption("c",  OPT_CALLBACK,          "Use class <callback> to bracket benchmark runs","callback"),
    makeOption("h",  OPT_HELP,              "Print this help",null),
    makeOption("r",  OPT_RELEASE_NOTES,     "Print the release notes",null),
    makeOption("l",  OPT_LIST_BENCHMARKS,   "List available benchmarks",null),
    makeOption("i",  OPT_INFORMATION,       "Display benchmark information",null),
    makeOption("s",  OPT_SIZE,              "Size of input data","SIZE"),
    makeOption(null, OPT_SCRATCH_DIRECTORY, "Specify an alternate scratch directory <dir>","dir"),
    makeOption("C",  OPT_CONVERGE,          "Allow benchmark times to converge before timing",null),
    makeOption(null, OPT_MAX_ITERATIONS,    "Run a max of <max_iterations> iterations (default 20)","max_iterations"),
    makeOption(null, OPT_VARIANCE,          "Target coefficient of variation <pct> (default 3.0)","pct"),
    makeOption(null, OPT_WINDOW,            "Measure variance over <window> runs (default 3)","window"),
    makeOption("n",  OPT_ITERATIONS,        "Run the benchmark <iter> times","iter"),
    makeOption("d",  OPT_DEBUG,             "Verbose debugging information",null),
    makeOption(null, OPT_IGNORE_VALIDATION, "Don't halt on validation failure",null),
    makeOption(null, OPT_NO_DIGEST_OUTPUT,  "Turn off SHA1 digest of stdout/stderr",null),
    makeOption(null, OPT_NO_VALIDATION,     "Don't validate at all",null),
    makeOption(null, OPT_PRESERVE,          "Preserve output files (debug)",null),
    makeOption(null, OPT_VALIDATION_REPORT, "Report digests, line counts etc","report_file"),
    makeOption(null, OPT_CONFIG,            null,"config_file"),
    makeOption("v",  OPT_VERBOSE,           "Verbose output",null)
  };
  
  private static CommandLineParser parser         = new PosixParser();
  private static Options           options        = new Options();
  private static Options           visibleOptions = new Options();

  {
    // Construct the option list and the visibleOption list.
    // The option list is used for parsing the command line, 
    // where as the visibleOption is a subset of the option list
    // and is used for producing the usage help.
    for(int i = 0; i<OPTIONS.length; i++) {
      options.addOption(OPTIONS[i]);
      if (OPTIONS[i].getDescription()!=null) visibleOptions.addOption(OPTIONS[i]);
    }
  }

  public enum Methodology { ITERATE, CONVERGE; }

  private CommandLine line;
  
  private boolean verbose = false; 
  private boolean debug = false;
  private boolean silent = true;
  private boolean allowOpenFromFileSystem = false;
  private Methodology methodology = Methodology.ITERATE;
  private double targetVar = DEFAULT_VARIANCE;
  private int window = DEFAULT_WINDOW_SIZE;
  private int maxIterations = DEFAULT_MAX_ITERATIONS;
  private boolean ignoreValidation = false;
  private int iterations = DEFAULT_ITERATIONS;
  private String size = DEFAULT_SIZE;
  private String scratchDir = DEFAULT_SCRATCH_DIRECTORY;
  private String cnf = null;
  private Callback callback = null;
  private boolean info = false;
  
  private List<String> benchmarks = new ArrayList<String>();

  CommandLineArgs(String[] args) throws Exception {
    try {
      boolean reportAndExitOk = false;
      line = parser.parse(options, args);
      
      if (line.hasOption(OPT_SIZE)) this.size = line.getOptionValue(OPT_SIZE);
      if (line.hasOption(OPT_INFORMATION)) this.info = true;
      if (line.hasOption(OPT_LIST_BENCHMARKS)) {
        printBenchmarks();
        reportAndExitOk = true;
      }
      if (line.hasOption(OPT_RELEASE_NOTES)) {
        printReleaseNotes();
        reportAndExitOk = true;
      }
      if (line.hasOption(OPT_HELP)) {
        printUsage();
        reportAndExitOk = true;
      }
      if (reportAndExitOk) System.exit(EXIT_OK);
      
      if (line.hasOption(OPT_VERBOSE)) {
        // display detailed information
        this.verbose = true;
        this.silent = false;
      }
      if (line.hasOption(OPT_CALLBACK)) {
        // use a callback
        Class<?> cls = null;
        try {
          cls = Class.forName(line.getOptionValue(OPT_CALLBACK));
        } catch (Exception e) {
          System.err.println(e);
          System.err.println("Could not find callback class "+line.getOptionValue(OPT_CALLBACK));
          System.exit(EXIT_MISSING_CALLBACK);
        } 
        if (!(Callback.class.isAssignableFrom(cls))) {
          System.err.println(line.getOptionValue(OPT_CALLBACK) + " is not an instance of Callback");
          System.exit(EXIT_BAD_CALLBACK);
        } else {
          Constructor<?> cons = cls.getConstructor(new Class[] { CommandLineArgs.class });
          this.callback = ((Callback) cons.newInstance(new Object[] {this}));
        }
      }
      if (line.hasOption(OPT_ITERATIONS)) this.iterations = Integer.parseInt(line.getOptionValue(OPT_ITERATIONS));
      if (line.hasOption(OPT_CONVERGE)) this.methodology = Methodology.CONVERGE;
      if (line.hasOption(OPT_CONFIG)) this.cnf = line.getOptionValue(OPT_CONFIG);     
      if (line.hasOption(OPT_MAX_ITERATIONS)) this.maxIterations = Integer.parseInt(line.getOptionValue(OPT_MAX_ITERATIONS));
      if (line.hasOption(OPT_VARIANCE)) this.targetVar = Double.parseDouble(line.getOptionValue(OPT_VARIANCE))/100.0;
      if (line.hasOption(OPT_WINDOW)) this.window = Integer.parseInt(line.getOptionValue(OPT_WINDOW));
      if (line.hasOption(OPT_DEBUG)) this.debug = true;
      if (line.hasOption(OPT_PRESERVE)) Benchmark.setPreserve(true);
      if (line.hasOption(OPT_NO_DIGEST_OUTPUT)) Benchmark.setValidateOutput(false);
      if (line.hasOption(OPT_IGNORE_VALIDATION)) this.ignoreValidation = true;
      if (line.hasOption(OPT_NO_VALIDATION)) Benchmark.setValidate(false);
      if (line.hasOption(OPT_VALIDATION_REPORT)) Benchmark.enableValidationReport(line.getOptionValue(OPT_VALIDATION_REPORT));
      if (line.hasOption(OPT_SCRATCH_DIRECTORY)) this.scratchDir = line.getOptionValue(OPT_SCRATCH_DIRECTORY);

      
    } catch (ParseException e) {
      System.err.println("Command line exception: " + e.getMessage());
      System.exit(EXIT_BAD_COMMANDLINE);
    } catch (Exception e) {
      System.err.println("Exception processing command line values: " + e.getMessage());
      System.exit(EXIT_BAD_COMMANDLINE);
    }
    
    // check that at least one benchmark is specified
    if (line.getArgList().isEmpty()) {
      printUsage();
      System.exit(EXIT_MISSING_BENCHMARKS);
    }
    
    // now get the benchmark names
    for(Object bm: line.getArgList()) {
      benchmarks.add((String)bm);
    }
    
    // Add the default callback
    if (getCallback() == null) {
      callback = new Callback(this);
    }
  }

  /**
   * Print a usage message to stdout
   */
  static void printUsage() {
    HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp("DaCapo Benchmark suite", visibleOptions);
  }
  
  /**
   * Print the release notes to System.out
   */
  static void printReleaseNotes() throws IOException {
    BufferedReader releaseNotes = new BufferedReader(new InputStreamReader(CommandLineArgs.class.getClassLoader().getResourceAsStream(RELEASE_NOTES)));

    String line;
    while ((line = releaseNotes.readLine()) != null) {
      System.out.println(line);
    }
  }
  
  /**
   * List all the benchmarks supported by this release
   */
  static void printBenchmarks() throws IOException {
    List<String> benchmarks = new ArrayList<String>();
    URL url = CommandLineArgs.class.getClassLoader().getResource("cnf");
    String protocol = url.getProtocol();
    if (protocol.equals("jar")) {
      JarURLConnection jarConnection = (JarURLConnection) url.openConnection();
      for (Enumeration<?> entries = jarConnection.getJarFile().entries(); entries.hasMoreElements(); ) {
        String entry = ((JarEntry) entries.nextElement()).getName();
        if (entry.endsWith(".cnf")) {
          entry = entry.replace("cnf/", "").replace(".cnf", "");
          benchmarks.add(entry);
        }
      }
    } else if (protocol.equals("file")) {
      File dir = new File(url.getFile());
      if (dir.isDirectory()) {
        File[] files = dir.listFiles();
        for (int i = 0; i < files.length; i++) {
          String entry = files[i].toString();
          entry = entry.substring(entry.lastIndexOf('/')+1, entry.length());
          entry = entry.replace(".cnf", "");
          benchmarks.add(entry);
        } 
      } 
    }
    Iterator<String> iter = benchmarks.iterator();
    
    for (; iter.hasNext(); ) {
      System.out.print(iter.next());
      if (iter.hasNext()) {
        System.out.print(" ");
      }
    }
    System.out.println();
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
  public String getCnfOverride() {
    return cnf;
  }
  public boolean isInfo() {
    return info;
  }
  public Iterable<String> benchmarks() {
    return benchmarks;
  }

  public boolean isSilent() {
    return silent;
  }

  public void setSilent(boolean silent) {
    this.silent = silent;
  }

  public boolean isDebug() {
    return debug;
  }
  
  /* Define a commandline option.
   * @param shortName An optional short form name for the command line option.
   * @param longname A commandline option must have a long form name.
   * @param description All commandline options that are visible options must have a
   *    description, commandline options that are for internal development usage must not
   *    have a description and must instead be documented in the code.
   * @param argName A commandline option that requires has an argument must specify an 
   *    argument name.
   */
  private static Option makeOption(String shortName, String longName, String description, String argName) {
    assert longName != null;
    
    Option option = new Option(shortName, longName, argName!=null, description); //(String opt, String longOpt, boolean hasArg, String description)
    
    if (argName!=null) {
      option.setValueSeparator('=');
      option.setArgName(argName);
    }
    
    return option;
  }
}
