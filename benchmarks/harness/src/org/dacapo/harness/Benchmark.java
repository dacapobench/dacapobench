/*
 * Copyright (c) 2006, 2009 The Australian National University.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License v2.0.
 * You may obtain the license at
 * 
 *    http://www.opensource.org/licenses/apache2.0.php
 */
package org.dacapo.harness;

import java.io.*;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.dacapo.parser.Config;

/**
 * Each DaCapo benchmark is represented by an instance of this abstract class.
 * It defines the methods that the benchmark harness calls during the running of
 * the benchmark.
 * 
 * @date $Date: 2009-12-24 11:19:36 +1100 (Thu, 24 Dec 2009) $
 * @id $Id: Benchmark.java 738 2009-12-24 00:19:36Z steveb-oss $
 */
public abstract class Benchmark {

  /*
   * Constants
   */

  /**
   * I/O buffer size for unzipping
   */
  private static final int BUFFER_SIZE = 2048;

  /*
   * Class variables
   */

  /**
   * Verbose output.
   */
  private static boolean verbose = false;

  /**
   * Display output from the benchmark ?
   */
  private static boolean silent = true;

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
   * Parsed version of the configuration file for this benchmark
   */
  protected final Config config;

  /**
   * Classloader used to run the benchmark
   */
  protected ClassLoader loader;

  /** Saved classloader across iterations */
  private ClassLoader savedClassLoader;

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

  /**
   * Run a benchmark. This is final because individual benchmarks should not
   * interfere with the flow of control.
   * 
   * @param callback The user-specified timing callback
   * @param size The size (as given on the command line)
   * @param timing Is this the timing loop ? Affects how we call the callback.
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

    startIteration();
    try {
      iterate(size);
    } finally {
      stopIteration();
    }

    callback.stop();

    boolean valid = validate(size);
    callback.complete(config.name, valid);
    postIteration(size);
    return valid;
  }

  public Benchmark(Config config, File scratch, boolean silent) throws Exception {
    // TODO this is very ugly
    Benchmark.silent = silent;
    this.scratch = scratch;
    this.config = config;
    initialize();
  }

  /**
   * When an instance of a Benchmark is created, it is expected to prepare its
   * scratch directory, unloading files from the jar file if required.
   * 
   * @param scratch Scratch directory
   */
  public Benchmark(Config config, File scratch) throws Exception {
    this(config, scratch, true);
  }

  protected void initialize() throws Exception {
    System.setProperty("java.util.logging.config.file", fileIn(scratch, config.name + ".log"));
    synchronized (System.out) {
      if (out == null) {
        out = new TeePrintStream(System.out, new File(scratch, "stdout.log"));
        out.enableOutput(!silent);
      }
    }
    synchronized (System.err) {
      if (err == null) {
        err = new TeePrintStream(System.err, new File(scratch, "stderr.log"));
        err.enableOutput(!silent);
      }
    }
    prepareJars();
    loader = DacapoClassLoader.create(config, scratch);
    prepare();
  }

  /**
   * Extract the jar files used by the benchmark
   * 
   * @throws Exception
   */
  private void prepareJars() throws IOException, FileNotFoundException, DacapoException {
    File file = new File(scratch + "/jar");
    if (!file.exists())
      file.mkdir();
    if (config.jar != null)
      extractFileResource("jar/" + config.jar, scratch);
    if (config.libs != null) {
      for (int i = 0; i < config.libs.length; i++) {
        extractFileResource("jar/" + config.libs[i], scratch);
      }
    }
  }

  /**
   * Perform pre-benchmark preparation. By default it unpacks the zip file
   * <code>data/<i>name</i>.zip</code> into the scratch directory.
   */
  protected void prepare() throws Exception {
    unpackZipFileResource("dat/" + config.name + ".zip", scratch);
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
      String[] args = config.preprocessArgs(size, scratch);
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
   * An actual iteration of the benchmark. This is what is timed.
   * 
   * @param args Arguments to the benchmark
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
          lines = lineCount(new File(scratch, file));
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
          bytes = byteCount(new File(scratch, file));
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
    if (!preserve) {
      postIterationCleanup(size);
    }
  }

  /**
   * Perform post-iteration cleanup.
   * 
   * @param size
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
   * Copy a file to the specified directory
   * 
   * @param inputFile File to copy
   * @param outputDir Destination directory
   */
  public static void copyFileTo(File inputFile, File outputDir) throws IOException {
    copyFile(inputFile, new File(outputDir, inputFile.getName()));
  }

  /**
   * Copy a file, specifying input and output file names.
   * 
   * @param inputFile Name of the input file.
   * @param outputFile Name of the output file
   * @throws IOException Any exception thrown by the java.io functions used to
   * perform the copy.
   */
  public static void copyFile(File inputFile, File outputFile) throws IOException {
    FileInputStream input = new FileInputStream(inputFile);
    FileOutputStream output = new FileOutputStream(outputFile);
    while (true) {
      byte[] buffer = new byte[BUFFER_SIZE];
      int read = input.read(buffer);
      if (read == -1)
        break;
      output.write(buffer, 0, read);
    }
    input.close();
    output.flush();
    output.close();
  }

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
    return fileIn(scratch, name);
  }

  private static String fileIn(File scratch, String name) {
    return (new File(scratch, name)).getPath();
  }

  /**
   * Unpack a zip archive into the specified directory.
   * 
   * @param name Name of the zip file
   * @param destination Directory to unpack into.
   * @throws IOException
   */
  public static void unpackZipFile(String name, File destination) throws IOException, FileNotFoundException {
    BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(name));
    unpackZipStream(inputStream, destination);
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
  private static void unpackZipStream(BufferedInputStream inputStream, File destination) throws IOException {
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

  public static void deleteFiles(File dir, final String pattern) {
    FilenameFilter filter = new FilenameFilter() {
      public boolean accept(File dir, String name) {
        return name.matches(pattern);
      }
    };
    File[] files = dir.listFiles(filter);
    for (int i = 0; i < files.length; i++) {
      deleteFile(files[i]);
    }
  }

  public static int lineCount(String file) throws IOException {
    return lineCount(new File(file));
  }

  public static int lineCount(File file) throws IOException {
    int lines = 0;
    BufferedReader in = new BufferedReader(new FileReader(file));
    while (in.readLine() != null)
      lines++;
    in.close();
    return lines;
  }

  public static long byteCount(String file) throws IOException {
    return byteCount(new File(file));
  }

  public static long byteCount(File file) throws IOException {
    return file.length();
  }

  public static void setCommandLineOptions(CommandLineArgs line) {
    silent = line.getSilent();
    preserve = line.getPreserve();
    validate = line.getValidate();
    validateOutput = line.getValidateOutput();
    preIterationGC = line.getPreIterationGC();
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

  public static boolean getSilent() {
    return silent;
  }

}
