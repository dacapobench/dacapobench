/**
 * 
 */
package org.dacapo.harness;

import java.io.File;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
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
import org.apache.commons.cli.Options;
import org.dacapo.parser.Config;

/**
 * Command line arguments for a dacapo benchmark run.
 * 
 * Encapsulated in an object so that it can be passed to user-written
 * callbacks.
 * 
 * @author Robin Garner
 * @author John Zigman
 *
 */
public class CommandLineArgs {
  
  private final static int   EXIT_OK                    = 0;
  private final static int   EXIT_MISSING_CALLBACK      = 2;
  private final static int   EXIT_BAD_CALLBACK          = 3;
  private final static int   EXIT_BAD_COMMANDLINE       = 4;
  private final static int   EXIT_UNKNOWN_BENCHMARK     = 9;
  private final static int   EXIT_MISSING_BENCHMARKS    = 10;
  
  private static final String RELEASE_NOTES             = "RELEASE_NOTES.txt";
  private static final String DEFAULT_SIZE              = "default";
  private static final String DEFAULT_SCRATCH_DIRECTORY = "./scratch";
  private static final String DEFAULT_MAX_ITERATIONS    = "20";
  private static final String DEFAULT_WINDOW_SIZE       = "3";
  private static final String DEFAULT_VARIANCE          = "3.0";
  private static final String DEFAULT_ITERATIONS        = "1";
  
  private static final String OPT_CALLBACK              = "callback";
  private static final String OPT_HELP                  = "help";
  private static final String OPT_RELEASE_NOTES         = "release-notes";
  private static final String OPT_LIST_BENCHMARKS       = "list-benchmarks";
  private static final String OPT_INFORMATION           = "information";
  private static final String OPT_SIZE                  = "size";
  private static final String OPT_SCRATCH_DIRECTORY     = "scratch-directory";
  private static final String OPT_CONVERGE              = "converge";
  private static final String OPT_MAX_ITERATIONS        = "max-iterations";
  private static final String OPT_VARIANCE              = "variance";
  private static final String OPT_WINDOW                = "window";
  private static final String OPT_ITERATIONS            = "iterations";
  private static final String OPT_DEBUG                 = "debug";
  private static final String OPT_IGNORE_VALIDATION     = "ignore-validation";
  private static final String OPT_NO_DIGEST_OUTPUT      = "no-digest-output";
  private static final String OPT_NO_VALIDATION         = "no-validation";
  private static final String OPT_PRESERVE              = "preserve";
  private static final String OPT_VALIDATION_REPORT     = "validation-report";
  private static final String OPT_CONFIG                = "config";
  private static final String OPT_VERBOSE               = "verbose";
  
  private static final Option[] OPTIONS = {
    makeOption("c",  OPT_CALLBACK,          "Use class <callback> to bracket benchmark runs",        "callback"),
    makeOption("h",  OPT_HELP,              "Print this help",                                       null),
    makeOption("r",  OPT_RELEASE_NOTES,     "Print the release notes",                               null),
    makeOption("l",  OPT_LIST_BENCHMARKS,   "List available benchmarks",                             null),
    makeOption("i",  OPT_INFORMATION,       "Display benchmark information",                         null),
    makeOption("s",  OPT_SIZE,              "Size of input data",                                    "SIZE"),
    makeOption(null, OPT_SCRATCH_DIRECTORY, "Specify an alternate scratch directory <dir>",          "dir"),
    makeOption("C",  OPT_CONVERGE,          "Allow benchmark times to converge before timing",       null),
    makeOption(null, OPT_MAX_ITERATIONS,    "Run a max of <max_iterations> iterations (default 20)", "max_iterations"),
    makeOption(null, OPT_VARIANCE,          "Target coefficient of variation <pct> (default 3.0)",   "pct"),
    makeOption(null, OPT_WINDOW,            "Measure variance over <window> runs (default 3)",       "window"),
    makeOption("n",  OPT_ITERATIONS,        "Run the benchmark <iter> times",                        "iter"),
    makeOption("d",  OPT_DEBUG,             "Verbose debugging information",                         null),
    makeOption(null, OPT_IGNORE_VALIDATION, "Don't halt on validation failure",                      null),
    makeOption(null, OPT_NO_DIGEST_OUTPUT,  "Turn off SHA1 digest of stdout/stderr",                 null),
    makeOption(null, OPT_NO_VALIDATION,     "Don't validate at all",                                 null),
    makeOption(null, OPT_PRESERVE,          "Preserve output files (debug)",                         null),
    makeOption(null, OPT_VALIDATION_REPORT, "Report digests, line counts etc",                       "report_file"),
    makeOption(null, OPT_CONFIG,            null,                                                    "config_file"),
    makeOption("v",  OPT_VERBOSE,           "Verbose output",                                        null)
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
  
  private Callback callback = null;

  private List<String> benchmarks = new ArrayList<String>();

  CommandLineArgs(String[] args) throws Exception {
    try {
      boolean reportAndExitOk = false;
      line = parser.parse(options, args);
      
      // report general benchmark information if requested and terminate
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
    } catch (ParseException e) {
      System.err.println("Command line exception: " + e.getMessage());
      System.exit(EXIT_BAD_COMMANDLINE);
    } catch (Exception e) {
      System.err.println("Exception processing command line values: " + e.getMessage());
      System.exit(EXIT_BAD_COMMANDLINE);
    }

    // configure the callback
    defineCallback();

    // check that at least one or more benchmarks are specified or a 
    // config file is specified but not both
    if (line.getArgList().isEmpty() && !line.hasOption(OPT_CONFIG)) {
      printUsage();
      System.exit(EXIT_MISSING_BENCHMARKS);
    } else if (line.hasOption(OPT_CONFIG) && !line.getArgList().isEmpty()) {
      System.err.println("You may only specify a cnf or a list of benchmarks, but not both");
      System.exit(EXIT_BAD_COMMANDLINE);
    }

    List<String> benchmarkSet = extractBenchmarkSet();

    if (! line.getArgList().isEmpty()) {
      // now get the benchmark names and check against the available benchmarks
      for(Object bm: line.getArgList()) {
        if (benchmarkSet.contains(bm))
          benchmarks.add((String)bm);
        else {
          System.err.println("Unknown benchmark: " + bm);
          System.exit(EXIT_UNKNOWN_BENCHMARK);
        }
      }
    } else {
      // determine benchmark from the cnf file and add it to the benchmarks
      File cnfFile = new File(line.getOptionValue(OPT_CONFIG));
      
      if (! cnfFile.canRead()) {
        System.err.println("Unknown config file: " + cnfFile.getAbsolutePath());
        System.exit(EXIT_UNKNOWN_BENCHMARK);
      }
      
      Config config = Config.parse(cnfFile);
      
      if (benchmarkSet.contains(config.name))
        benchmarks.add(config.name);
      else {
        System.err.println("Unknown benchmark specified in cnf file: " + config.name);
        System.exit(EXIT_UNKNOWN_BENCHMARK);
      }
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
    Iterator<String> iter = extractBenchmarkSet().iterator();
    
    for (; iter.hasNext(); ) {
      System.out.print(iter.next());
      if (iter.hasNext()) {
        System.out.print(" ");
      }
    }
    System.out.println();
  }
  
  static List<String> extractBenchmarkSet() throws IOException {
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
    return benchmarks;
  }
  
  /*
   * Getter methods
   */
  
  public boolean isVerbose() {
    return line.hasOption(OPT_VERBOSE);
  }
  public Methodology methodology() {
    return line.hasOption(OPT_CONVERGE)?Methodology.CONVERGE:Methodology.ITERATE;
  }
  public boolean convergeMethodology() {
    return methodology() == Methodology.CONVERGE;
  }
  public boolean iterateMethodology() {
    return methodology() == Methodology.ITERATE;
  }
  public double getTargetVar() {
    return Double.parseDouble(line.getOptionValue(OPT_VARIANCE,DEFAULT_VARIANCE))/100.0; // targetVar;
  }
  public int getWindow() {
    return Integer.parseInt(line.getOptionValue(OPT_WINDOW,DEFAULT_WINDOW_SIZE)); // window;
  }
  public int getMaxIterations() {
    return Integer.parseInt(line.getOptionValue(OPT_MAX_ITERATIONS,DEFAULT_MAX_ITERATIONS)); // maxIterations;
  }
  public boolean isIgnoreValidation() {
    return line.hasOption(OPT_IGNORE_VALIDATION); // ignoreValidation;
  }
  public int getIterations() {
    return Integer.parseInt(line.getOptionValue(OPT_ITERATIONS,DEFAULT_ITERATIONS)); // iterations;
  }
  public String getSize() {
    return line.getOptionValue(OPT_SIZE, DEFAULT_SIZE);
  }
  public String getScratchDir() {
    return line.getOptionValue(OPT_SCRATCH_DIRECTORY, DEFAULT_SCRATCH_DIRECTORY);
  }
  public Callback getCallback() {
    return callback;
  }
  public String getCnfOverride() {
    return line.getOptionValue(OPT_CONFIG,null);
  }
  public boolean isInfo() {
    return line.hasOption(OPT_INFORMATION); // info;
  }
  public Iterable<String> benchmarks() {
    return benchmarks;
  }

  public boolean isSilent() {
    return !isVerbose();
  }

  public boolean isDebug() {
    return line.hasOption(OPT_DEBUG);
  }
  
  public boolean getPreserve() {
    return line.hasOption(OPT_PRESERVE);
  }
  
  public boolean getValidateOutput() {
    return !line.hasOption(OPT_NO_DIGEST_OUTPUT); // validateOutput;
  }

  public boolean getValidate() {
    return !line.hasOption(OPT_NO_VALIDATION);
  }

  public String getValidationReport() {
    return line.getOptionValue(OPT_VALIDATION_REPORT,null);
  }

  private void defineCallback() throws SecurityException, NoSuchMethodException, IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {
    // define the callback class (or set the default if none specified
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
    
    // set the default callback class if no callback is defined
    if (getCallback() == null) {
      callback = new Callback(this);
    }
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
    
    Option option = new Option(shortName, longName, argName!=null, description);
    
    if (argName!=null) {
      option.setValueSeparator('=');
      option.setArgName(argName);
    }
    
    return option;
  }
}
