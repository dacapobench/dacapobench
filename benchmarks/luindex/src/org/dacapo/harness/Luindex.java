package org.dacapo.harness;

import java.io.File;
import java.lang.reflect.Constructor;

import org.dacapo.harness.Benchmark;
import org.dacapo.harness.DacapoException;
import org.dacapo.parser.Config;

/**
 * Dacapo benchmark harness for Lucene.  This is a modified version of
 * org.apache.lucene.demo.IndexFiles, as distributed with Lucene.
 * 
 * @author Apache
 * @author Robin Garner
 *
 */

public class Luindex extends Benchmark {
  
  private final Object benchmark;
  
  public Luindex(Config config, File scratch) throws Exception {
    super(config,scratch);
    Class<?> clazz = Class.forName("org.dacapo.luindex.Index", true, loader);
    this.method = clazz.getMethod("main", File.class, String[].class);
    Constructor<?> cons = clazz.getConstructor(File.class);
    useBenchmarkClassLoader();
    try {
      benchmark = cons.newInstance(scratch);
    } finally {
      revertClassLoader();
    }
  }
  
  public void cleanup() {
    if (!isPreserve()) {
      deleteTree(new File(scratch,"luindex"));
    }
  }
  
  public void preIteration(String size) {
    if (isPreserve() && getIteration() > 1) {
      deleteTree(new File(scratch,"index"));
    }
  }
  
  /** 
   * Index all text files under a directory. 
   */
  public void iterate(String size) throws Exception {
    if (isVerbose())
      System.out.println("luindex benchmark starting");
    String[] args = preprocessArgs(size);
    
    final File INDEX_DIR = new File(scratch,"index");
    
    if (INDEX_DIR.exists()) {
      System.out.println("Cannot save index to '" +INDEX_DIR+ "' directory, please delete it first");
      throw new DacapoException("Cannot write to index directory");
    }

    method.invoke(benchmark, INDEX_DIR, args);
  }

  public void postIteration(String size) {
    if (!isPreserve()) {
      deleteTree(new File(scratch,"index"));
    }
  }
}
