package org.dacapo.harness;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.dacapo.harness.Benchmark;
import org.dacapo.parser.Config;

public class Pmd extends Benchmark {

  String[] args;

  public Pmd(Config config, File scratch) throws Exception {
    super(config, scratch);
    Class<?> clazz = Class.forName("net.sourceforge.pmd.PMD", true, loader);
    this.method = clazz.getMethod("main", String[].class);

    /*
     * Explicitly set some properties that control factory methods
     * 
     * Leaving it to the standard methods of resolving at runtime can lead to
     * testing different implementations on different platforms.
     * 
     * It's always possible that there are additional properties that need to be
     * set.
     */
    System.setProperty("javax.xml.parsers.DocumentBuilderFactory",
        "org.apache.xerces.jaxp.DocumentBuilderFactoryImpl");
  }

  public void prepare(String size) {
    args = config.getArgs(size);
    if (args[0].charAt(0) == '@')
      args[0] = collectFilesFromFile(fileInScratch(args[0].substring(1)));
    for (int i = 2; i < args.length; i++) {
      if (args[i].charAt(0) != '-')
        args[i] = fileInScratch(args[i]);
    }
  }

  public void iterate(String size) throws Exception {
    method.invoke(null, (Object) args);
  }

  private String collectFilesFromFile(String inputFileName) {
    try {
      java.io.BufferedReader reader = new java.io.BufferedReader(
          new InputStreamReader(new FileInputStream(inputFileName)));

      List<File> files = new ArrayList<File>();

      for (String l = reader.readLine(); l != null; l = reader.readLine()) {
        files.add(new File(scratch, l));
      }
      return commaSeparate(files);
    } catch (FileNotFoundException e) {
      throw new RuntimeException("File " + inputFileName + " error: " + e);
    } catch (java.io.IOException e) {
      throw new RuntimeException("File " + inputFileName + " error: " + e);
    }

  }

  private static String commaSeparate(List<File> list) {
    String result = "";
    for (Iterator<File> i = list.iterator(); i.hasNext();) {
      String s = i.next().getPath();
      result += s;
      if (i.hasNext())
        result += ",";
    }
    return result;
  }

  /**
   * Stub which exists <b>only</b> to facilitate whole program static analysis
   * on a per-benchmark basis. See also the "split-deps" ant build target, which
   * is also provided to enable whole program static analysis.
   * 
   * @author Eric Bodden
   */
  public static void main(String args[]) throws Exception {
    // create dummy harness and invoke with dummy arguments
    (new Pmd(null, null)).run(null, "");
  }
}
