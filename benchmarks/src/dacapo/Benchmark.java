package dacapo;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import dacapo.parser.Config;

/**
 * Each DaCapo benchmark is represented by an instance of this
 * abstract class.  It defines the methods that the benchmark harness
 * calls during the running of the benchmark.
 * 
 * @author Robin Garner
 *
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
   * Perform digest operations on standard output and standard error
   */
  private static boolean validateOutput = true;
  
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
  private int iteration = 0;
  
  /**
   * Run a benchmark.  This is final because individual
   * benchmarks should not interfere with the flow of control.
   * 
   * @param callback The user-specified timing callback
   * @param size The size (as given on the command line)
   * @param timing Is this the timing loop ?  Affects how we call the callback.
   * @return Whether the run was valid or not.
   * @throws Exception Whatever exception the target application dies with
   */
  public final boolean run(Callback callback, String size, boolean timing) throws Exception {
    iteration++;
    preIteration(size);
    if (timing)
      callback.start(config.name);
    else
      callback.startWarmup(config.name);
    
    startIteration();
    iterate(size);
    stopIteration();
    
    if (timing)
      callback.stop();
    else
      callback.stopWarmup();
    
    boolean valid = validate(size);
    if (timing)
      callback.complete(config.name, valid);
    else
      callback.completeWarmup(config.name, valid);
    postIteration(size);
    return valid;
  }
  
  /**
   * When an instance of a Benchmark is created, it is expected to prepare 
   * its scratch directory, unloading files from the jar file if required.
   * 
   * @param scratch Scratch directory
   */
  public Benchmark(Config config, File scratch) throws Exception {
    this.scratch = scratch;
    this.config = config;
    if (validate) {
      synchronized(System.out) {
        if (out == null) 
          out = new TeePrintStream(System.out,new File(scratch,"stdout.log"));
      }
      synchronized(System.err) {
        if (err == null) 
          err = new TeePrintStream(System.err,new File(scratch,"stderr.log"));
      }
    }
    prepare();
  }
  
  /**
   * Perform pre-benchmark preparation.  By default it unpacks the zip file
   * <code>data/<i>name</i>.zip</code> into the scratch directory.
   */
  protected void prepare() throws Exception {
    unpackZipFileResource("data/"+config.name+".zip", scratch);
  }
  
  /**
   * Benchmark-specific per-iteration setup, outside the timing loop.
   * 
   * Needs to take care of any *required* cleanup when the -preserve
   * flag us used.
   * 
   * @param size Size as specified by the "-s" command line flag
   */
  public void preIteration(String size) throws Exception {
    if (verbose) {
      String[] args = config.getArgs(size);
      System.out.print("Benchmark parameters: ");
      for (int i=0; i < args.length; i++)
        System.out.print(args[i]+" ");
      System.out.println();
    }
    
    /* 
     * Allow those benchmarks that can't tolerate overwriting prior output
     * to run in the face of the '-preserve' flag.
     */
    if (preserve && iteration > 1)
      postIterationCleanup(size);
  }
  
  /**
   * Per-iteration setup, inside the timing loop.  Nothing comes between this and
   * the call to 'iterate' - its purpose is to start collection of the input
   * and output streams.  stopIteration() should be its inverse.
   */
  public final void startIteration() {
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
  }
  
  /**
   * An actual iteration of the benchmark.  This is what is
   * timed.
   * 
   * @param args Arguments to the benchmark
   */
  public abstract void iterate(String size) throws Exception;

  /**
   * Post-iteration tear-down, inside the timing loop.  Restores standard output
   * and error, and saves the digest of the iteration output.  This is inside
   * the timing loop so as not to process any output from the timing harness.
   */
  public final void stopIteration() {
    if (validateOutput) {
      out.closeLog();
      err.closeLog();
      System.setOut(savedOut);
      System.setErr(savedErr);
    }
  }
  
  /**
   * Perform validation of output.  By default process the conditions
   * specified in the config file.
   * 
   * @param size Size of the benchmark run.
   * @return true if the output was correct
   */
  public boolean validate(String size) {
    if (!validate) 
      return true;
    
    if (validationReport) {
      valRepFile.println("Validating "+config.name+" "+size);
    }
    boolean valid = true;
    for (Iterator v = config.getOutputs(size).iterator(); v.hasNext(); ) {
      String file = (String)v.next();

      /*
       * Validate by file digest
       */
      if (config.hasDigest(size,file)) {
        String refDigest = config.getDigest(size,file);
        String digest;

        try {
          digest = Digest.toString(FileDigest.get(fileInScratch(file),
              config.isTextFile(size, file),
              config.filterScratch(size, file),
              scratch));
        } catch (FileNotFoundException e) {
          digest = "<File not found>";
        } catch (IOException e) {
          digest = "<IO exception>";
          e.printStackTrace();
        }
        if (validationReport) {
          valRepFile.println("  \""+file+"\" digest 0x"+digest+",");
        }
        if (!validateOutput && (file.equals("$stdout") || file.equals("$stderr")) ) {
          // Not collecting digests for stdout and stderr, so can't check them
        } else if (!digest.equals(refDigest)) {
          valid = false;
          System.err.println("Digest validation failed for "+file+", expecting 0x"+refDigest+" found 0x"+digest);
        } else if (verbose) { 
          System.out.println("Digest validation succeeded for "+file);
        }
      }
      
      /*
       * Validate by line count
       */
      if (config.hasLines(size,file)) {
        int refLines = config.getLines(size,file);
        int lines;
        try {
          lines = lineCount(new File(scratch,file));
        } catch (FileNotFoundException e) {
          System.err.println("File not found, "+file);
          lines = -1;
        } catch (IOException e) {
          e.printStackTrace();
          lines = -1;
        }
        if (validationReport) {
          valRepFile.println("  \""+file+"\" lines "+lines+",");
        }
        if (lines != refLines) {
          valid = false;
          System.err.println("Line count validation failed for "+file+", expecting "+refLines+" found "+lines);
        } else if (verbose) { 
          System.out.println("Line count validation succeeded for "+file);
        }
      }
      
      /*
       * Validate by byte count
       */
      if (config.hasBytes(size,file)) {
        long refBytes = config.getBytes(size,file);
        long bytes;
        try {
          bytes = byteCount(new File(scratch,file));
        } catch (FileNotFoundException e) {
          System.err.println("File not found, "+file);
          bytes = -1;
        } catch (IOException e) {
          e.printStackTrace();
          bytes = -1;
        }
        if (validationReport) {
          valRepFile.println("  \""+file+"\" bytes "+bytes+",");
        }
        if (bytes != refBytes) {
          valid = false;
          System.err.println("Byte count validation failed for "+file+", expecting "+refBytes+" found "+bytes);
        } else if (verbose) { 
          System.out.println("Byte count validation succeeded for "+file);
        }
      }
      
      /*
       * Check for existence
       */
      if (config.checkExists(size, file)) {
        if (!new File(scratch,file).exists()) {
          System.err.println("Expected file "+file+" does not exist");
          valid = false;
        } else if (verbose) { 
          System.out.println("Existence validation succeeded for "+file);
        }
      }
    }
    if (validationReport) {
      valRepFile.flush();
    }
    return valid;
  }
  
  /**
   * Per-iteration cleanup, outside the timing loop.  By default it 
   * deletes the named output files.
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
    for (Iterator v = config.getOutputs(size).iterator(); v.hasNext(); ) {
      String file = (String)v.next();
      if (file.equals("$stdout") || file.equals("$stderr")) {
      } else {
        if (!config.isKept(size,file))
          deleteFile(new File(scratch,file));
      }
    }
  }
  
  /**
   * Perform post-benchmark cleanup, deleting output files etc.
   * By default it deletes a subdirectory of the scratch directory with
   * the same name as the benchmark.
   */
  public void cleanup() {
    if (!preserve) {
      deleteTree(new File(scratch,config.name));
    }
  }
  
  /*************************************************************************************
   *  Utility methods
   */
  
  /**
   * Copy a file to the specified directory
   * 
   * @param inputFile File to copy
   * @param outputDir Destination directory
   */
  public static void copyFileTo(File inputFile, File outputDir) throws IOException {
    copyFile(inputFile,new File(outputDir,inputFile.getName()));
  }
  
  /**
   * Copy a file, specifying input and output file names.
   * 
   * @param inputFile Name of the input file.
   * @param outputFile Name of the output file
   * @throws IOException Any exception thrown by the java.io functions used
   *                     to perform the copy.
   */
  public static void copyFile(File inputFile, File outputFile) throws IOException {
    FileInputStream input = new FileInputStream(inputFile);
    FileOutputStream output = new FileOutputStream(outputFile);
    while (true) {
      byte[] buffer = new byte[BUFFER_SIZE];
      int read = input.read(buffer);
      if (read == -1) break;
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
      System.out.println("Util.getURL: returns "+resource);
    return resource;    
  }
  
  /**
   * Return a file name, relative to the specified scratch directory.
   * 
   * @param name Name of the file, relative to the top of the scratch directory
   * @return The path name of the file
   */
  public String fileInScratch(String name) {
    return (new File(scratch,name)).getPath();
  }

  /**
   * Unpack a zip archive into the specified directory.
   * 
   * @param name Name of the zip file
   * @param destination Directory to unpack into.
   * @throws IOException
   */
  public static void unpackZipFile(String name, File destination) throws 
  IOException, FileNotFoundException {
    BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(name));
    unpackZipStream(inputStream, destination);
  }

  /**
   * Unpack a zip file resource into the specified directory.  The directory structure
   * of the zip archive is preserved.
   * 
   * @param name
   * @param destination
   * @throws IOException
   */
  public static void unpackZipFileResource(String name, File destination) 
  throws IOException, FileNotFoundException, DacapoException {
    URL resource = getURL(name);
    if (resource == null)
      throw new DacapoException("No such zip file: \""+name+"\"");
    
    BufferedInputStream inputStream = new BufferedInputStream(resource.openStream());
    unpackZipStream(inputStream, destination);
  }

  /**
   * @param inputStream
   * @param destination
   * @throws IOException
   */
  private static void unpackZipStream(BufferedInputStream inputStream, File destination) 
  throws IOException {
    ZipInputStream input = new ZipInputStream(inputStream);
    ZipEntry entry;
    while((entry = input.getNextEntry()) != null) {
      if (verbose)
        System.out.println("Unpacking "+entry.getName());
      File file = new File(destination,entry.getName());
      if (entry.isDirectory()) {
        if (!file.exists())
          file.mkdir();
      } else {
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
    }
    input.close();
  }

  public static void deleteTree(File tree) {
    if (verbose) 
      System.out.println("Deleting "+tree.getName());
    if (!tree.isDirectory())
      tree.delete();
    else {
      File[] files = tree.listFiles();
      for (int i=0; i < files.length; i++)
        deleteTree(files[i]);
      tree.delete();
    }
  }
  
  public static void deleteFile(File file) {
    if (verbose) 
      System.out.println("Deleting "+file.getName());
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
    for (int i=0; i < files.length; i++) {
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
  
  /**
   * Sort an array.
   * 
   * @param list
   * @return
   */
  protected void sortArray(Comparable[] list) {
    List l = new ArrayList(list.length);
    for (int i=0; i < list.length; i++)
      l.add(list[i]);
    Collections.sort(l);
    int j = 0;
    for (Iterator i = l.iterator(); i.hasNext(); ) {
      list[j++] = (Comparable)i.next();
      //System.out.println(j+" : "+list[j-1]);
    }
  }
  
  public static long byteCount(String file) throws IOException {
    return byteCount(new File(file));
  }
  public static long byteCount(File file) throws IOException {
    return file.length();
  }

  public static void setVerbose(boolean verbose) {
    Benchmark.verbose = verbose;
  }

  public static boolean isVerbose() {
    return verbose;
  }

  public static void enableValidationReport(String filename) {
    try {
      validationReport = true;
      // Append to an output file
      valRepFile = new PrintWriter(new BufferedWriter(new FileWriter(filename,true)));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static void setValidateOutput(boolean digestOutput) {
    Benchmark.validateOutput = digestOutput;
  }

  public static boolean isDigestOutput() {
    return validateOutput;
  }

  public static void setPreserve(boolean preserve) {
    Benchmark.preserve = preserve;
  }

  public static boolean isPreserve() {
    return preserve;
  }

  /**
   * @return the iteration
   */
  protected int getIteration() {
    return iteration;
  }

  /**
   * @param validate the validate to set
   */
  public static void setValidate(boolean flag) {
    validate = flag;
    setValidateOutput(false);
  }
}
