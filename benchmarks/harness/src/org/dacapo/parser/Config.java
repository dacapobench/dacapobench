/**
 * Copyright (c) Dept of Computer Science, ANU
 */
package org.dacapo.parser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.Vector;

/**
 * Container class for all options specified in a benchmark's configuration
 * file.
 *
 * @author Robin Garner
 */
public class Config {

  public enum ThreadModel {
    SINGLE("single threaded"),
    FIXED("fixed #threads"),
    PER_CPU("scaled to available CPUs");

    private String description;
    private ThreadModel(String description) { this.description = description; }
    public String describe() { return description; }
  };

  /**
   * Inner class that keeps details of one of the output files
   * specified by a benchmark.
   */
  class OutputFile {
    String name;                   // Output file name
    String digest = null;          // SHA1-digest - check if non-null
    boolean keep = false;          // keep this file
    boolean existence = false;     // Check file exists
    int lines = -1;                // Check for #lines
    long bytes = -1;               // Check for #bytes

    /* Options that apply to digest processing */
    boolean text = false;          // Read as a text file - canonical CR/LF processing
    boolean filter = false;        // Filter scratch directory name

    OutputFile(String name) {
      this.name = name;
    }
    boolean hasDigest() { return digest != null; }
    boolean hasLines() { return lines != -1; }
    boolean hasBytes() { return bytes != -1; }
  }

  /**
   * Inner class to encapsulate specifications for a given sized run of
   * a benchmark.
   *
   * @author Robin Garner
   *
   */
  class Size {
    final String name;
    final String[] args;

    /**
     * Number of threads.  Interpretation depends on the threading model of the benchmark
     * <ul>
     * <li> <code>SINGLE</code> One thread
     * <li> <code>FIXED</code> the exact number of threads used.
     * <li> <code>PER_CPU</code> A multiplier on the number of CPUs detected or specified
     * </ul>
     */
    private int nThreads = 1;

    HashMap<String,OutputFile> outputFiles = new LinkedHashMap<String,OutputFile>(20);

    Size(String name, Vector<String> args) {
      this.args = (String[])args.toArray(new String[0]);
      this.name = name;
    }
    void addOutputFile(String file) {
      outputFiles.put(file,new OutputFile(file));
    }
    OutputFile getOutputFile(String file) {
      return (OutputFile)outputFiles.get(file);
    }

    void setThreadCount(int nThreads) { this.nThreads = nThreads; }
    int getThreadCount() { return nThreads; }
  }

  /*********************************************************************************
   *
   *               Class methods.  Factory methods that invoke the
   *               parser on various input sources
   */

  /**
   * Parse a config file
   *
   * @param file
   * @return
   */
  public static Config parse(String file) { return parse(new File(file)); }

  /**
   * Parse a config file
   *
   * @param file
   * @return
   */
  public static Config parse(File file) {
    try {
      return parse(new FileInputStream(file));
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
    return null;
  }

  /**
   * Parse a config file
   *
   * @param url
   * @return
   */
  public static Config parse(URL url) {
    try {
      return parse(url.openStream());
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }

  public static Config parse(InputStream stream) {
    try {
      ConfigFile parser = new ConfigFile(stream);

      return parser.configFile();
    } catch (ParseException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return null;
  }


  /**
   * Main - for testing purposes.  Parse a config file and print its contents.
   *
   * @param args Input file(s)
   */
  public static void main(String[] args) {
    // TODO Auto-generated method stub
    for (int i=0; i < args.length; i++) {
      parse(args[i]).print(System.out);
    }
  }


  /********************************************************************************
   *
   *                 Instance fields
   */

  /**
   * The name of the benchmark
   */
  public final String name;

  /**
   * The class name of the benchmark driver
   */
  public String className = null;

  /**
   * The threading model used by the benchmark
   */
  private ThreadModel threadModel = null;

  HashMap<String,Size> sizes = new HashMap<String,Size>(3);
  HashMap<String,String> desc = new HashMap<String,String>(6);

  /**
   * The name of the jar containing this bm
   */
  public String jar;

  /**
   * The list of jars upon which this bm depends
   */
  public String[] libs;

  /**
   * Constructor.  These are always constructed by the parser, and
   * at time of construction all we know is the benchmark name.
   *
   * @param name
   */
  Config(String name) {
    this.name = name;
  }

  /**************************************************************************************
   *
   *            Per-benchmark setter methods.  Invoked as the
   *            parser finds out more about the benchmark.
   *
   */

  /**
   * Set the jar from which this benchmark executes
   *
   * @param jarName The name of the jar
   * @throws ParseException
   */
  void setJar(String jarName) throws ParseException {
    if (this.jar != null) {
      throw new ParseException("Configuration file error - cannot set jar name twice");
    }
    this.jar = jarName;
  }

  /**
   * Set the list of libraries on which this benchmark depends
   *
   * @param libs An array of strings (jar names)
   * @throws ParseException
   */
  void setLibs(String[] libs) throws ParseException {
    if (this.libs != null) {
      throw new ParseException("Configuration file error - cannot set libs twice");
    }
    this.libs = libs;
  }

  /**
   * Set the class name - easier to check for duplicate attempts here than in the grammar.
   *
   * @param className
   * @throws ParseException
   */
  void setClass(String className) throws ParseException {
    if (this.className != null) {
      throw new ParseException("Configuration file error - cannot set class name twice");
    }
    this.className = className;
  }

  /**
   * Set the threading model for this benchmark
   *
   * @param model
   * @throws ParseException
   */
  void setThreadModel(ThreadModel model) throws ParseException {
    if (this.threadModel != null) {
      throw new ParseException("Configuration file error - cannot set thread model twice");
    }
    this.threadModel = model;
  }

  /**
   * Add a benchmark run size
   *
   * @param name
   * @param args
   */
  void addSize(String name, Vector<String> args) {
    sizes.put(name,new Size(name,args));
  }

  /**
   * Add a description element for the benchmark
   *
   * @param element
   * @param description
   */
  void addDesc(String element, String description) {
    desc.put(element,description);
  }

  /*************************************************************************************
   *
   *   Per-config getter methods
   *
   */


  /***************************************************************************************
   *
   *          Per-size setter methods
   *
   */

  /**
   * Set the threading factor for this size.
   */
  void setThreadFactor(String size, int nThreads) throws ParseException {
    if (threadModel == ThreadModel.SINGLE && nThreads != 1)
      throw new ParseException("Single threaded benchmarks must have exactly 1 thread");
    getSize(size).setThreadCount(nThreads);
  }


  /*************************************************************************************
   * Output files
   */

  /**
   * Add an output file to this size of the benchmark
   */
  void addOutputFile(String size, String file) {
    getSize(size).addOutputFile(file);

    /* Set defaults for certain files */
    if (file.equals("stdout.log") || file.equals("stderr.log")) {
      setTextFile(size,file,true);
      setFilterScratch(size,file,true);
    }
  }

  /** Set the expected digest for an output file */
  void setDigest(String size, String file, String digest) {
    getSize(size).getOutputFile(file).digest = digest;
  }

  /** Set the expected line count for an output file */
  void setLines(String size, String file, int lines) {
    getSize(size).getOutputFile(file).lines = lines;
  }

  /** Set the expected byte count for an output file */
  void setBytes(String size, String file, long bytes) {
    getSize(size).getOutputFile(file).bytes = bytes;
  }

  /** Set whether we keep this file around after the benchmark run */
  void setKeep(String size, String file) {
    getSize(size).getOutputFile(file).keep = true;
  }

  /** Set whether we check for existence of this file */
  void setExists(String size, String file) {
    getSize(size).getOutputFile(file).existence = true;
  }

  /**
   * Is this a text file (affects how it is read)
   *
   * @param size benchmark size
   * @param file output file
   * @param isText Is this a text file ?
   */
  public void setTextFile(String size, String file, boolean isText) {
    Size s = getSize(size);
    s.getOutputFile(file).text = isText;
  }

  /**
   * Do we filter this file replacing occurrences of the scratch directory
   * name before applying the digest function to it ?
   *
   * @param size
   * @param file
   * @param doFilter
   */
  public void setFilterScratch(String size, String file, boolean doFilter) {
    Size s = getSize(size);
    s.getOutputFile(file).filter = doFilter;
  }

  /***********************************************************************************
   *
   *                        Getter methods
   */

  /**
   * Benchmark arguments for a given run size.  The return value
   * is a clone so callers are free to modify it.
   */
  public String[] getArgs(String size) {
    return (String[])getSize(size).args.clone();
  }

  /**
   * Get the collection of sizes this benchmark accepts
   *
   * @return A collection of strings
   */
  public Collection<String> getSizes() {
    return Collections.unmodifiableSet(sizes.keySet());
  }

  /**
   * Get the thread model for this benchmark.  Apply the default here,
   * because we use 'null' to prevent double-setting in the config file.
   */
  public ThreadModel getThreadModel() {
    if (threadModel == null) {
      return ThreadModel.FIXED;
    } else {
      return threadModel;
    }
  }

  /**
   * Get the threading factor for this size of this benchmark.
   *
   * @param size
   * @return
   */
  public int getThreadFactor(String size) {
    return getSize(size).getThreadCount();
  }

  /**
   * Get the set of output files for a benchmark size
   *
   * @param size
   * @return Set\<String\> of file names
   */
  public Set<String> getOutputs(String size) {
    return Collections.unmodifiableSet(getSize(size).outputFiles.keySet());
  }

  /**
   * Get the expected digest for a given size/file pair
   * @param size benchmark size
   * @param file output file
   */
  public String getDigest(String size, String file) {
    return getSize(size).getOutputFile(file).digest;
  }

  /**
   * Does the given size/file pair have an expected file digest ?
   * @param size benchmark size
   * @param file output file
   */
  public boolean hasDigest(String size, String file) {
    return getSize(size).getOutputFile(file).hasDigest();
  }

  /**
   * Is this a text file (affects how it is read)
   *
   * @param size benchmark size
   * @param file output file
   * @return Is this a text file ?
   */
  public boolean isTextFile(String size, String file) {
    Size s = getSize(size);
    return s.getOutputFile(file).text;
  }

  /**
   * Should we filter scratch directories for this output file ?
   * @param size
   * @param file
   * @return
   */
  public boolean filterScratch(String size, String file) {
    return getSize(size).getOutputFile(file).filter;
  }

  /**
   * Does this output file have a byte length validation ?
   *
   * @param size
   * @param file
   * @return
   */
  public boolean hasBytes(String size, String file) {
    return getSize(size).getOutputFile(file).hasBytes();
  }

  /**
   * What is the byte-length requirement for this file ?
   *
   * @param size
   * @param file
   * @return
   */
  public long getBytes(String size, String file) {
    return getSize(size).getOutputFile(file).bytes;
  }

  public boolean hasLines(String size, String file) {
    Size s = getSize(size);
    return s.getOutputFile(file).hasLines();
  }

  public int getLines(String size, String file) {
    Size s = getSize(size);
    return s.getOutputFile(file).lines;
  }

  public boolean isKept(String size, String file) {
    Size s = getSize(size);
    return s.getOutputFile(file).keep;
  }

  /**
   * Should we check for the existence of this file ?
   * @param size
   * @param file
   * @return
   */
  public boolean checkExists(String size, String file) {
    Size s = getSize(size);
    return s.getOutputFile(file).existence;
  }

  private String pad(String in, int length) {
    while (in.length() < length)
      in += " ";
    return in;
  }

  /*
   * Manage the description fields
   */
  public void describe(PrintStream str) { describe(str,false); }

  private void describe(PrintStream str, boolean decorated,String desc,String trail) {
    if (decorated) str.print("  ");
    str.println(pad(desc,10)+this.desc.get(desc)+(decorated?trail:""));
  }

  public void describe(PrintStream str, boolean decorated) {
    if (decorated) str.println("description");
    describe(str,decorated,"short",",");
    describe(str,decorated,"long",",");
    describe(str,decorated,"author",",");
    describe(str,decorated,"license",",");
    describe(str,decorated,"copyright",",");
    describe(str,decorated,"url",",");
    describe(str,decorated,"version",";");
  }

  public String getDesc(String item) {
    return (String)desc.get(item);
  }

  public void print(PrintStream str) {
    str.print("benchmark "+name);
    if (className != null)
      str.print(" class "+className);
    str.println(";");

    str.print("  Threading model: ");
    if (threadModel == null) {
      str.println("unspecified");
    } else {
      str.println(threadModel.describe());
    }

    for (String size : getSizes()) {
      String[] args = getArgs(size);
      str.print("size "+size+" args \"");
      for (int j=0; j < args.length; j++) {
        if (j != 0)
          str.print(" ");
        str.print(args[j]);
      }
      str.println("\"");
      str.print("  threads ");
      if (threadModel == null) {
        str.println("<specified in benchmark arguments>");
      } else {
        str.println(getThreadFactor(size));
      }
      str.print("  outputs");
      for (Iterator<String> v = getOutputs(size).iterator(); v.hasNext(); ) {
        str.println();
        String file = (String)v.next();
        OutputFile f = getSize(size).getOutputFile(file);
        str.print("    \""+file+"\"");
        if (f.hasDigest()) str.print(" digest 0x"+f.digest);
        if (f.keep) str.print(" keep");
        if (v.hasNext())
          str.print(",");
      }
      str.println(";");
    }

    describe(str,true);
  }

  /*************************************************************************************
   *
   *                       Utility methods
   *
   */

  /**
   * Extract the named size from the available sizes in this benchmark, handling pesky
   * epistemological issues.
   *
   * @param size
   * @return
   */
  private Size getSize(String size) {
    Size s = (Size)sizes.get(size);
    if (s == null) {
      System.err.println("No such size: \""+size+"\" in this configuration");
    }
    return s;
  }

}
