/*
 * Copyright (c) 2006, 2009, 2020 The Australian National University.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License v2.0.
 * You may obtain the license at
 * 
 *    http://www.opensource.org/licenses/apache2.0.php
 */
package org.dacapo.harness;

import java.io.*;
import java.nio.file.Files;
import java.security.MessageDigest;
import javax.xml.bind.DatatypeConverter;

import java.lang.reflect.Method;
import java.net.URL;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.TreeSet;
import java.util.ArrayList;
import java.util.Collections;

import org.dacapo.parser.Config;

/**
 * Each DaCapo benchmark is represented by an instance of this abstract class.
 * It defines the methods that the benchmark harness calls during the running of
 * the benchmark.
 * 
 * date:  $Date: 2009-12-24 11:19:36 +1100 (Thu, 24 Dec 2009) $
 * id: $Id: Benchmark.java 738 2009-12-24 00:19:36Z steveb-oss $
 */
public abstract class Benchmark {

  /*
   * Constants
   */

  /**
   * I/O buffer size for unzipping
   */
  private static final int BUFFER_SIZE = 2048;
  /**
   * Timeout Dialation property.
   */
  private static final String TIMEOUT_DIALATION_PROPERTY = "dacapo.timeout.dialation";

  /*
   * Class variables
   */

  /**
   * Verbose output.
   */
  private static boolean verbose = false;

  /**
   * How many iterations will run
   */
  protected static int iterations = 1;

  /**
   * Display stdout from the benchmark ?
   */
  private static boolean silentOut = true;

  /**
   * Display stderr from the benchmark ?
   */
  private static boolean silentErr = true;

  /**
   * Perform digest operations on standard output and standard error
   */
  private static boolean validateOutput = true;

  /**
   * Perform System.gc() just prior to each iteration
   */
  private static boolean preIterationGC = false;

  /**
   * Perform validation
   */
  private static boolean validate = true;

  /**
   * Don't clean up output files
   */
  private static boolean preserve = false;

  /**
   * Output file for writing digests
   */
  private static PrintWriter valRepFile = null;

  /**
   *
   */
  private static boolean validationReport = false;

  /**
   * Factor used to increase the timeouts used in a benchmark.
   * Note that it's impact is dependent on the particular benchmark
   * utilizing this timeout.dialation property.
   */
  protected static String timeoutDialation = "1";
  
  /**
   * Should we dump latency stats to csv file
   */
  private static boolean dumpLatencyCSV = false;

  /**
   * Should we dump latency stats to hdr file
   */
  private static boolean dumpLatencyHDR = false;

  /**
   * Base name for latency files
   */
  private static String latencyBaseFileName = null;

  /**
   * Saved System.out while redirected to the digest stream
   */
  private static final PrintStream savedOut = System.out;

  /**
   * Saved System.err while redirected to the digest stream
   */
  private static final PrintStream savedErr = System.err;

  /*
   * Instance fields
   */

  /**
   * The scratch directory
   */
  protected final File scratch;

  /**
   * The data directory
   */
  protected final File data;

  /**
   * Parsed version of the configuration file for this benchmark
   */
  protected final Config config;

  /**
   * if this benchmark require a data set
   */
  private final boolean dataSet;

  /**
   * Classloader used to run the benchmark
   */
  protected ClassLoader loader;

  /** Saved classloader across iterations */
  private ClassLoader savedClassLoader;

  /**
   * The system properties that were in effect when the harness was started.
   * 
   * @see System#getProperties()
   */
  private Properties savedSystemProperties;

  /**
   * Output stream for validating System.err
   */
  private static TeePrintStream err = null;

  /**
   * Output stream for validating System.out
   */
  private static TeePrintStream out = null;

  /**
   * Keep track of the number of times we have been iterated.
   */
  protected int iteration = 0;

  protected Method method;

  private Set<URL> jarDeps = new HashSet();
  private Set<URL> datDeps = new HashSet();
  private Map<String, Integer> stats = new HashMap();

  /**
   * Run a benchmark. This is final because individual benchmarks should not
   * interfere with the flow of control.
   * 
   * @param callback The user-specified timing callback
   * @param size The size (as given on the command line)
   * @return Whether the run was valid or not.
   * @throws Exception Whatever exception the target application dies with
   */
  public final boolean run(Callback callback, String size) throws Exception {
    iteration++;
    if (iteration == 1) {
      prepare(size);

      // this rather obscure addition is here in case the preparation stage
      // of a benchmark manipulates System.out and System.err
      System.setOut(savedOut);
      System.setErr(savedErr);
    }

    preIteration(size);
    // again we may have to correct a benchmarks manipulation of the
    // Systemout and Sytem.err during the preIteration phase of the benchmark
    System.setOut(savedOut);
    System.setErr(savedErr);

    if (preIterationGC) {
      System.gc();
    }

    callback.start(config.name);

    final long start = System.currentTimeMillis();

    startIteration();
    try {
      iterate(size);
    } finally {
      stopIteration();
    }

    final long duration = System.currentTimeMillis() - start;

    callback.stop(duration);

    boolean valid = validate(size);
    callback.complete(config.name, valid);
    postIteration(size);
    return valid;
  }

  /**
   * When an instance of a Benchmark is created, it is expected to prepare its
   * scratch directory, unloading files from the jar file if required.
   * 
   * @param scratch Scratch directory
   */
  public Benchmark(Config config, File scratch, File data) throws Exception {
    this(config, scratch, data, true);
  }

  public Benchmark(Config config, File scratch, File data, boolean silent) throws Exception {
    this(config, scratch, data, silent, silent);
  }

  public Benchmark(Config config, File scratch, File data, boolean silentOut, boolean silentErr) throws Exception {
    this(config, scratch, data, silentOut, silentErr, true);
  }

  public Benchmark(Config config, File scratch, File data, boolean silentOut, boolean silentErr, boolean dataSet) throws Exception {
    Benchmark.silentOut = silentOut;
    Benchmark.silentErr = silentErr;

    this.scratch = scratch;
    this.data = data;
    this.config = config;
    this.dataSet = dataSet;
    javaVersionCheck();
    initialize();
  }

  private void initialize() throws Exception {
    savedSystemProperties = System.getProperties();
    Data.checkData(data);

    System.setProperty("java.util.logging.config.file", fileInScratch(config.name + ".log"));
    synchronized (System.out) {
      if (out == null) {
        out = new TeePrintStream(System.out, new File(scratch, "stdout.log"));

        out.enableOutput(!silentOut);
      }
    }
    synchronized (System.err) {
      if (err == null) {
        err = new TeePrintStream(System.err, new File(scratch, "stderr.log"));
        err.enableOutput(!silentErr);
      }
    }
    if (!getDeps("META-INF/md5/" + config.name  + ".MD5"))
      System.exit(-1);

    if (!loadStats("META-INF/yml/" + config.name  + ".yml"))
      System.exit(-1);

    loader = DacapoClassLoader.create(config, scratch, data, jarDeps);
    prepare();
  }

  protected void javaVersionCheck() {}

  /**
   * Take a benchmark's list of dependencies and:
   *  - check that the specified files exist
   *  - check that the files have the correct checksums
   *  - add the path to the file to the jar or data dependency set.
   * returning true if successful.
   * 
   * @param dependencyFile Path to the file of dependencies to be checked.
   * @return True if successful, false otherwise.
   */
  private boolean getDeps(String dependencyFile) {
    InputStream in = Benchmark.class.getClassLoader().getResourceAsStream(dependencyFile);
    if (in == null) {
      System.err.println("Can't find MD5 list for benchmark: " + config.name);
      System.exit(20);
    }
    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
    boolean success = reader.lines().map(l -> {

      String md5Expected = l.substring(0, 32).toLowerCase();
      String filePath = l.substring(33);
      try {
        File f = new File(data, filePath);
        if (!f.exists()) {
          System.out.println("Missing data file: "+data+File.separator+filePath);
          return false;
        }

        if (filePath.startsWith("jar")) {
          jarDeps.add(f.toURI().toURL());
        } else {
          datDeps.add(f.toURI().toURL());
        }

        String md5 = getMD5(f).toLowerCase();
        if (!md5.equals(md5Expected)) {
          System.out.println("Checksum failure: expected "+md5Expected+" for "+data+File.separator+filePath+" but got "+md5);
          return false;
        }
      } catch (Exception e) {
        System.out.println("Dependency check failure: did not find expected file "+data+File.separator+filePath);
        return false;
      }
      return true;
    }).reduce(true, (a, b) -> a & b);

    try {
      reader.close();
      in.close();
    } catch (Exception e) {
      System.out.println("Error closing dependency file '"+dependencyFile+"': "+e);
    }
    return success;
  }

  /**
   * Return the MD5 sum for the specified file.
   */
  private static String getMD5(File file) throws Exception{
    MessageDigest md = MessageDigest.getInstance("MD5");
    byte [] buffer = new byte [1024 * 64]; // 64KB buffer
    InputStream stream = Files.newInputStream(file.toPath());
    int bytesRead = 0;
    while ((bytesRead = stream.read(buffer)) != -1) {
      md.update(buffer, 0, bytesRead);
    }
    stream.close();
    return DatatypeConverter.printHexBinary(md.digest());
  }

  /**
   * Perform pre-benchmark preparation.
   */
  protected void prepare() throws Exception {
    System.out.println("Version: "+config.getDesc("version"));
    System.out.println("Nominal stats: "+getStats());
  }

  /* read yml by hand for now since snakeyml (introduced in ed3b413b) led to inexplicable failures of trade benchmarks */
  private boolean loadStats(String ymlFile) {
    try (BufferedReader in = new BufferedReader(new InputStreamReader(Benchmark.class.getClassLoader().getResourceAsStream(ymlFile)))) {
      String line;
      if (in.ready()) {
        line = in.readLine();
        if (line.startsWith("stats:")) {
          while (in.ready()) {
            line = in.readLine();
            int idx = line.indexOf('#');
            if (idx != -1)
              line = line.substring(0,idx);
            String[] tokens = line.trim().split(": ");
            try {
              stats.put(tokens[0], Integer.parseInt(tokens[1]));
            } catch (NumberFormatException nfe) {
              System.err.println("Badly formatted line '"+line+"' in file "+ymlFile);
              break;
            }
          }
          in.close();
          return true; // successfully parsed
        }
      }
    } catch (Exception e) {
      System.err.println("Failed to load stats file from yml "+ymlFile+" "+e);
    }
    return false;
  }

  private String getStats() {
    String rtn = "";
    for (String key : new TreeSet<>(stats.keySet())) {
      if (rtn.length() != 0) { rtn += ", "; }
      rtn += key+": "+stats.get(key);
    }
    return rtn;
  }

  /**
   * One-off preparation performed once we know the benchmark size.
   * 
   * By default, does nothing.
   * 
   * @param size The size (as defined in the per-benchmark configuration file).
   */
  protected void prepare(String size) throws Exception {
  }

  /**
   * Benchmark-specific per-iteration setup, outside the timing loop.
   * 
   * Needs to take care of any *required* cleanup when the -preserve flag us
   * used.
   * 
   * @param size Size as specified by the "-s" command line flag
   */
  public void preIteration(String size) throws Exception {
    if (verbose) {
      String[] args = config.preprocessArgs(size, scratch, data);
      System.out.print("Benchmark parameters: ");
      for (int i = 0; i < args.length; i++)
        System.out.print(args[i] + " ");
      System.out.println();
    }
  
    /*
     * Allow those benchmarks that can't tolerate overwriting prior output to
     * run in the face of the '-preserve' flag.
     */
    if (preserve && iteration > 1)
      postIterationCleanup(size);
  }

  /**
   * Per-iteration setup, inside the timing loop. Nothing comes between this and
   * the call to 'iterate' - its purpose is to start collection of the input and
   * output streams. stopIteration() should be its inverse.
   */
  public final void startIteration() {
    if (verbose) {
      System.out.println("startIteration()");
    }
    System.setProperty(TIMEOUT_DIALATION_PROPERTY, Benchmark.timeoutDialation);

    final Properties augmentedSystemProperties = (Properties) savedSystemProperties.clone();
    augmentSystemProperties(augmentedSystemProperties);
    System.setProperties(augmentedSystemProperties);

    if (validateOutput) {
      System.setOut(out);
      System.setErr(err);
      if (iteration > 1) {
        out.version();
        err.version();
      }
      out.openLog();
      err.openLog();
    }
    useBenchmarkClassLoader();
  }

  /**
   * Augments the system properties in case additional properties need to be in
   * effect during the actual benchmark iteration.
   * 
   * @param systemProperties the system properties that need to be augmented.
   *   (They may be modified freely.)
   */
  public void augmentSystemProperties(Properties systemProperties) { }

  /**
   * An actual iteration of the benchmark. This is what is timed.
   * 
   * @param size Argument to the benchmark iteration.
   */
  public abstract void iterate(String size) throws Exception;

  /**
   * Post-iteration tear-down, inside the timing loop. Restores standard output
   * and error, and saves the digest of the iteration output. This is inside the
   * timing loop so as not to process any output from the timing harness.
   */
  public final void stopIteration() {
    revertClassLoader();
    if (validateOutput) {
      out.closeLog();
      err.closeLog();
      System.setOut(savedOut);
      System.setErr(savedErr);
    }

    System.setProperties(savedSystemProperties);

    if (verbose) {
      System.out.println("stopIteration()");
    }
  }

  /**
   * TODO
   */
  protected void useBenchmarkClassLoader() {
    if (loader != null) {
      savedClassLoader = Thread.currentThread().getContextClassLoader();
      Thread.currentThread().setContextClassLoader(loader);
    }
  }

  /**
   * TODO
   */
  protected void revertClassLoader() {
    if (loader != null) {
      Thread.currentThread().setContextClassLoader(savedClassLoader);
    }
  }

  /**
   * Perform validation of output. By default process the conditions specified
   * in the config file.
   * 
   * @param size Size of the benchmark run.
   * @return true if the output was correct
   */
  public boolean validate(String size) {
    if (verbose) {
      System.out.println("validate(" + validate + ")");
    }
    if (!validate)
      return true;

    if (validationReport) {
      valRepFile.println("Validating " + config.name + " " + size);
    }
    boolean valid = true;
    for (String file : config.getOutputs(size)) {

      /*
       * Validate by file digest
       */
      if (config.hasDigest(size, file)) {
        String refDigest = config.getDigest(size, file);
        String digest;

        try {
          digest = Digest.toString(FileDigest.get(fileInScratch(file), config.isTextFile(size, file), config.filterScratch(size, file), scratch));
        } catch (FileNotFoundException e) {
          digest = "<File not found>";
        } catch (IOException e) {
          digest = "<IO exception>";
          e.printStackTrace();
        }
        if (validationReport) {
          valRepFile.println("  \"" + file + "\" digest 0x" + digest + ",");
        }
        if (!validateOutput && (file.equals("$stdout") || file.equals("$stderr"))) {
          // Not collecting digests for stdout and stderr, so can't check them
        } else if (!digest.equals(refDigest)) {
          valid = false;
          System.err.println("Digest validation failed for " + file + ", expecting 0x" + refDigest + " found 0x" + digest);
        } else if (verbose) {
          System.out.println("Digest validation succeeded for " + file);
        }
      }

      /*
       * Validate by line count
       */
      if (config.hasLines(size, file)) {
        int refLines = config.getLines(size, file);
        int lines;
        try {
          File tempFile = new File(scratch, file);
          if (!tempFile.exists())
            throw new FileNotFoundException();
          lines = lineCount(tempFile);
        } catch (FileNotFoundException e) {
          System.err.println("File not found, " + file);
          lines = -1;
        } catch (IOException e) {
          e.printStackTrace();
          lines = -1;
        }
        if (validationReport) {
          valRepFile.println("  \"" + file + "\" lines " + lines + ",");
        }
        if (lines != refLines) {
          valid = false;
          System.err.println("Line count validation failed for " + file + ", expecting " + refLines + " found " + lines);
        } else if (verbose) {
          System.out.println("Line count validation succeeded for " + file);
        }
      }

      /*
       * Validate by byte count
       */
      if (config.hasBytes(size, file)) {
        long refBytes = config.getBytes(size, file);
        long bytes;
        try {
          File genSeg = new File(scratch, file);
          if (!genSeg.exists())
            throw new FileNotFoundException();
          bytes = byteCount(genSeg);
        } catch (FileNotFoundException e) {
          System.err.println("File not found, " + file);
          bytes = -1;
        } catch (IOException e) {
          e.printStackTrace();
          bytes = -1;
        }
        if (validationReport) {
          valRepFile.println("  \"" + file + "\" bytes " + bytes + ",");
        }
        if (bytes != refBytes) {
          valid = false;
          System.err.println("Byte count validation failed for " + file + ", expecting " + refBytes + " found " + bytes);
        } else if (verbose) {
          System.out.println("Byte count validation succeeded for " + file);
        }
      }

      /*
       * Check for existence
       */
      if (config.checkExists(size, file)) {
        if (!new File(scratch, file).exists()) {
          System.err.println("Expected file " + file + " does not exist");
          valid = false;
        } else if (verbose) {
          System.out.println("Existence validation succeeded for " + file);
        }
      }
    }
    if (validationReport) {
      valRepFile.flush();
    }
    return valid;
  }

  /**
   * Per-iteration cleanup, outside the timing loop. By default it deletes the
   * named output files.
   * 
   * @param size Argument to the benchmark iteration.
   */
  public void postIteration(String size) throws Exception {
    LatencyReporter.reportLatency(latencyBaseFileName, dumpLatencyCSV, dumpLatencyHDR, iteration);
    if (!preserve) {
      postIterationCleanup(size);
    }
  }
 
  /**
   * Perform post-iteration cleanup.
   * 
   * @param size Argument to the benchmark iteration.
   */
  protected void postIterationCleanup(String size) {
    for (String file : config.getOutputs(size)) {
      if (file.equals("$stdout") || file.equals("$stderr")) {
      } else {
        if (!config.isKept(size, file))
          deleteFile(new File(scratch, file));
      }
    }
  }

  /**
   * Perform post-benchmark cleanup, deleting output files etc. By default it
   * deletes a subdirectory of the scratch directory with the same name as the
   * benchmark.
   */
  public void cleanup() {
    if (!preserve) {
      deleteTree(new File(scratch, config.name));
    }
  }

  /*************************************************************************************
   * Utility methods
   */

  /**
   * Translate a resource name into a URL.
   * 
   * @param fn
   * @return
   */
  public static URL getURL(String fn) {
    ClassLoader cl = Benchmark.class.getClassLoader();
    URL resource = cl.getResource(fn);
    if (verbose)
      System.out.println("Util.getURL: returns " + resource);
    return resource;
  }

  /**
   * Return a file name, relative to the specified scratch directory.
   * 
   * @param name Name of the file, relative to the top of the scratch directory
   * @return The path name of the file
   */
  public String fileInScratch(String name) {
    return new File(scratch, name).getPath();
  }

  /**
   * Return a file name, relative to the specified data directory.
   *
   * @param name Name of the file, relative to the top of the data directory
   * @return The path name of the file
   */
  public String fileInData(String name) {
    return new File(data, name).getPath();
  }


  /**
   * Unpack a zip file resource into the specified directory. The directory
   * structure of the zip archive is preserved.
   * 
   * @param name
   * @param destination
   * @throws IOException
   */
  public static void unpackZipFileResource(String name, File destination) throws IOException, FileNotFoundException, DacapoException {
    URL resource = getURL(name);
    if (resource == null)
      throw new DacapoException("No such zip file: \"" + name + "\"");

    BufferedInputStream inputStream = new BufferedInputStream(resource.openStream());
    unpackZipStream(inputStream, destination);
  }

  public static void extractFileResource(String name, File destination) throws IOException, FileNotFoundException, DacapoException {
    if (verbose)
      System.out.println("Extracting file " + name + " into " + destination.getCanonicalPath());
    URL resource = getURL(name);
    if (resource == null)
      throw new DacapoException("No such file: \"" + name + "\"");
    BufferedInputStream inputStream = new BufferedInputStream(resource.openStream());
    fileFromInputStream(inputStream, new File(destination, name));
  }

  /**
   * @param inputStream
   * @param destination
   * @throws IOException
   */
  public static void unpackZipStream(BufferedInputStream inputStream, File destination) throws IOException {
    ZipInputStream input = new ZipInputStream(inputStream);
    ZipEntry entry;
    while ((entry = input.getNextEntry()) != null) {
      if (verbose)
        System.out.println("Unpacking " + entry.getName());
      File file = new File(destination, entry.getName());
      if (entry.isDirectory()) {
        if (!file.exists())
          file.mkdir();
      } else {
        fileFromInputStream(input, file);
      }
    }
    input.close();
  }

  private static void fileFromInputStream(InputStream input, File file) throws IOException {
    FileOutputStream fos = new FileOutputStream(file);
    BufferedOutputStream dest = new BufferedOutputStream(fos, BUFFER_SIZE);
    int count;
    byte data[] = new byte[BUFFER_SIZE];
    while ((count = input.read(data, 0, BUFFER_SIZE)) != -1) {
      dest.write(data, 0, count);
    }
    dest.flush();
    dest.close();
  }

  public static void deleteTree(File tree) {
    if (verbose)
      System.out.println("Deleting " + tree.getName());
    if (!tree.isDirectory())
      tree.delete();
    else {
      File[] files = tree.listFiles();
      for (int i = 0; i < files.length; i++)
        deleteTree(files[i]);
      tree.delete();
    }
  }

  public static void deleteFile(File file) {
    if (verbose)
      System.out.println("Deleting " + file.getName());
    if (file.exists() && !file.isDirectory())
      file.delete();
  }

  public static int lineCount(File file) throws IOException {
    int lines = 0;
    BufferedReader in = new BufferedReader(new FileReader(file));
    while (in.readLine() != null)
      lines++;
    in.close();
    return lines;
  }

  public static long byteCount(File file) throws IOException {
    return file.length();
  }

  public static void setCommandLineOptions(CommandLineArgs line) {
    silentOut = line.getSilent();
    silentErr = line.getSilent();
    iterations = line.getIterations();
    preserve = line.getPreserve();
    validate = line.getValidate();
    validateOutput = line.getValidateOutput();
    preIterationGC = line.getPreIterationGC();
    timeoutDialation = line.getTimeoutDialation();
    latencyBaseFileName = new File(line.getLogDirectory(), "dacapo-latency").getAbsolutePath();
    dumpLatencyCSV = line.getLatencyCSV();
    dumpLatencyHDR = line.getLatencyHDR();

    if (line.getValidationReport() != null)
      Benchmark.enableValidationReport(line.getValidationReport());
  }

  private static void enableValidationReport(String filename) {
    try {
      validationReport = true;
      // Append to an output file
      valRepFile = new PrintWriter(new BufferedWriter(new FileWriter(filename, true)));
    } catch (IOException e) {
      e.printStackTrace();
      System.exit(1);
    }
  }

  // getter methods
  public static boolean getVerbose() {
    return verbose;
  }

  public static boolean getValidateOutput() {
    return validateOutput;
  }

  public static boolean getValidate() {
    return validate;
  }

  public static boolean getPreserve() {
    return preserve;
  }

  protected int getIteration() {
    return iteration;
  }

  public static boolean getSilentOut() {
    return silentOut;
  }

  public static boolean getSilentErr() {
    return silentErr;
  }

  static final int UNPARSABLE_VERSION = -1;
  public void assertJavaVersionEQ(int version, String message) {
    if (version != getJavaVersion())
      incorrectJavaVersion(message);
  }

  public void assertJavaVersionGE(int version, String message) {
    if (getJavaVersion() < version)
      incorrectJavaVersion(message);
  }

  public void assertJavaVersionLE(int version, String message) {
    if (getJavaVersion() > version)
      incorrectJavaVersion(message);
  }

  public void warnJavaVersionLE(int version, String message) {
    if (getJavaVersion() > version)
      System.err.println("WARNING: the Java version string provided by this JVM is '"+System.getProperty("java.version")+"'. "+message);
  }

  private void incorrectJavaVersion(String message) {
    if (getJavaVersion() == UNPARSABLE_VERSION) {
      System.err.println("WARNING: "+config.name+" will only run with specific Java versions.   However the Java version string provided by this JVM ('"+System.getProperty("java.version")+"') could not be parsed.  It is therefore unclear whether this JVM is compatable.  The particular requirement for "+config.name+" is as follows: '"+message+"'");
    } else {
      System.err.print("Java version '"+System.getProperty("java.version") + "' is incompatable with "+config.name+": ");
      System.err.println(message);
      System.err.println("Exiting.");
      System.exit(-1);
    }
  }

  private static int getJavaVersion() {
    String version = System.getProperty("java.version");
    if (version.endsWith("-internal")) {
      version = version.substring(0, version.indexOf("-internal"));
    }
    if (version.startsWith("1.")) {
        version = version.substring(2, 3);
    } else {
        int dot = version.indexOf(".");
        if(dot != -1) { version = version.substring(0, dot); }
    }
    int v;
    try {
      v = Integer.parseInt(version);
    } catch (NumberFormatException e) {
      v = UNPARSABLE_VERSION;
    }
    return v;
  }
}
