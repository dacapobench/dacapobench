package dacapo;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.util.Iterator;
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
   * These flags are set by the TestHarness in response to command line flags
   */
  public static boolean verbose = false;
  public static boolean digestOutput = true;
  public static boolean preserve = false;
  
  protected final File scratch;
  protected final Config config;
  
  private static final int BUFFER = 2048;
  protected static final MessageDigest outDigest = Digest.create();
  protected static final MessageDigest errDigest = Digest.create();
  protected static final PrintStream out = new PrintStream(
      new DigestOutputStream(System.out,outDigest));
  protected static final PrintStream err = new PrintStream(
      new DigestOutputStream(System.err,errDigest));
  
  private static PrintStream savedOut = System.out;
  private static PrintStream savedErr = System.err;
  
  private String lastOutDigest, lastErrDigest;
  
  public boolean run(Callback callback, String size, boolean timing) throws Exception {
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
    prepare();
  }
  
  /**
   * Perform pre-benchmark preparation.  By default it unpacks the zip file
   * data/<code>name</code>.zip into the scratch directory.
   */
  protected void prepare() throws Exception {
    unpackZipFileResource("data/"+config.name+".zip", scratch);
  }
  
  /**
   * Benchmark-specific per-iteration setup, outside the timing loop.
   * 
   * @param size
   */
  public void preIteration(String size) throws Exception {
    if (verbose) {
      String[] args = config.getArgs(size);
      for (int i=0; i < args.length; i++)
        System.out.print(args[i]+" ");
      System.out.println();
    }
  }
  
  /**
   * Per-iteration setup, inside the timing loop.  Nothing comes between this and
   * the call to 'iterate' - its purpose is to start collection of the input
   * and output streams.  stopIteration() should be its inverse.
   */
  public final void startIteration() {
    if (digestOutput) {
      System.setOut(out);
      System.setErr(err);
      outDigest.reset();
      errDigest.reset();
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
    if (digestOutput) {
      System.setOut(savedOut);
      System.setErr(savedErr);
      lastOutDigest = Digest.toString(outDigest.digest());
      lastErrDigest = Digest.toString(errDigest.digest());
    }
  }
  
  /**
   * Perform validation of output
   * 
   * @param size Size of the benchmark run.
   * @return true if the output was correct
   */
  public boolean validate(String size) {
    boolean valid = true;
    for (Iterator v = config.getOutputs(size).iterator(); v.hasNext(); ) {
      String file = (String)v.next();
      
      /*
       * Validate by file digest
       */
      if (config.hasDigest(size,file)) {
        String refDigest = config.getDigest(size,file);
        String digest;
        if (file.equals("$stdout")) {
          digest = lastOutDigest;
        } else if (file.equals("$stderr")) {
          digest = lastErrDigest;
        } else {
          try {
            digest = Digest.toString(FileDigest.get(fileInScratch(file)));
          } catch (FileNotFoundException e) {
            digest = "<File not found>";
          } catch (IOException e) {
            digest = "<IO exception>";
            e.printStackTrace();
          }
        }
        if (!digestOutput && (file.equals("$stdout") || file.equals("$stderr")) ) {
          // Not collecting digests for stdout and stderr, so can't check them
        } else if (!digest.equals(refDigest)) {
          valid = false;
          System.err.println("Digest validation failed for "+file+", expecting "+refDigest+" found "+digest);
        } else if (verbose) { 
          System.out.println("Digest validation succeeded for "+file);
        }
      }
      
      /*
       * Validate by line count
       */
      if (config.hasLines(size,file)) {
        if (file.equals("$stdout") || file.equals("$stderr"))
          System.err.println("Line count not supported for error/output streams");
        else {
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
          if (lines != refLines) {
            valid = false;
            System.err.println("Line count validation failed for "+file+", expecting "+refLines+" found "+lines);
          } else if (verbose) { 
            System.out.println("Line count validation succeeded for "+file);
          }
        }
      }
      
      /*
       * Validate by byte count
       */
      if (config.hasBytes(size,file)) {
        if (file.equals("$stdout") || file.equals("$stderr"))
          System.err.println("Byte count not supported for error/output streams");
        else {
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
          if (bytes != refBytes) {
            valid = false;
            System.err.println("Byte count validation failed for "+file+", expecting "+refBytes+" found "+bytes);
          } else if (verbose) { 
            System.out.println("Byte count validation succeeded for "+file);
          }
        }
      }
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
      for (Iterator v = config.getOutputs(size).iterator(); v.hasNext(); ) {
        String file = (String)v.next();
        if (file.equals("$stdout") || file.equals("$stderr")) {
        } else {
          if (!config.isKept(size,file))
            deleteFile(new File(scratch,file));
        }
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
  
  public static void copyFileTo(File inputFile, File outputDir) throws IOException {
    copyFile(inputFile,new File(outputDir,inputFile.getName()));
  }
  
  public static void copyFile(File inputFile, File outputFile) throws IOException {
    FileInputStream input = new FileInputStream(inputFile);
    FileOutputStream output = new FileOutputStream(outputFile);
    while (true) {
      byte[] buffer = new byte[BUFFER];
      int read = input.read(buffer);
      if (read == -1) break;
      output.write(buffer, 0, read);
    }
    input.close();
    output.flush();
    output.close();
  }
  
  /**
   * @param fn
   * @return
   */
  public static URL getURL(String fn) {
    ClassLoader cl = Benchmark.class.getClassLoader();
    if (verbose)
      System.out.println("Util.getURL: returns "+cl.getResource(fn));
    return cl.getResource(fn);    
  }
  
  public String fileInScratch(String name) {
    return (new File(scratch,name)).getPath();
  }

  public static void unpackZipFile(String name, File destination) throws Exception {
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
  public static void unpackZipFileResource(String name, File destination) throws Exception {
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
   * @throws FileNotFoundException
   */
  private static void unpackZipStream(BufferedInputStream inputStream, File destination) throws IOException, FileNotFoundException {
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
        BufferedOutputStream dest = new BufferedOutputStream(fos, BUFFER);
        int count;
        byte data[] = new byte[BUFFER];
        while ((count = input.read(data, 0, BUFFER)) != -1) {
          //System.out.write(x);
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
  
  public static long byteCount(String file) throws IOException {
    return byteCount(new File(file));
  }
  public static long byteCount(File file) throws IOException {
    return file.length();
  }
  
}
