package org.dacapo.harness;

import java.io.File;
import java.util.Vector;

import org.dacapo.harness.Benchmark;
import org.dacapo.parser.Config;

/**
 * Benchmark harness for the Antlr benchmark
 *
 * @author Robin Garner
 * @author Steve Blackburn
 * @date $Date: 2008-07-27 12:53:06 +1000 (Sun, 27 Jul 2008) $
 * @id $Id: Antlr.java 397 2008-07-27 02:53:06Z steveb-oss $
 *
 */
public class Antlr extends Benchmark {

  private String[] args;
  private int firstGrammarIndex;
  private int iterations;
  private String[] antlrToolArgs;
  
  public Antlr(Config config, File scratch) throws Exception {
    super(config,scratch);
    Class<?> clazz = Class.forName("antlr.Tool", true, loader);
    this.method = clazz.getMethod("main", new Class[] { String[].class} );

  }

  protected void prepare() throws Exception {
    super.prepare();
    copyFileTo(new File(scratch,"antlr/CommonTokenTypes.txt"),scratch);
  }
  
  

  @Override
  public void prepare(String size) throws Exception {
    super.prepare(size);
    String[] args = preprocessArgs(size);
    firstGrammarIndex = 0;
    iterations = 1;
    Vector<String> v = new Vector<String>(args.length);
    for (int i=0; i < args.length; i++) {
      if (args[i].equals("-grammars")) {
        firstGrammarIndex = i+1;
        antlrToolArgs = new String[v.size()+1];
        for (int j=0; j < v.size(); j++) {
          antlrToolArgs[j] = (String)v.elementAt(j);
        }
        break;
      } else if (args[i].equals("-iterations")) {
        iterations = Integer.parseInt(args[++i]);
      } else {
        v.addElement(args[i]);
      }
    }
    this.args = args;
  }

  /**
   * After each iteration, delete the output files
   */
  public void postIteration(String size) throws Exception {
    super.postIteration(size);
    if (!isPreserve())
      deleteTree(new File(scratch,"antlr/output"));
  }

  public void iterate(String size) throws Exception {
    for (int iteration=0; iteration < iterations; iteration++) {
      for (int iGrammar=firstGrammarIndex;
           iGrammar < args.length;
           iGrammar++) {
        String grammarFile = (new File(scratch,args[iGrammar])).getPath();
        antlrToolArgs[antlrToolArgs.length-1] = grammarFile;
        System.out.println(args[iGrammar]);

        method.invoke(null, new Object[] {antlrToolArgs});
      }
    }
  }
  
  /**
   * Stub which exists <b>only</b> to facilitate whole program
   * static analysis on a per-benchmark basis.  See also the "split-deps"
   * ant build target, which is also provided to enable whole program
   * static analysis.
   * 
   * @author Eric Bodden
   */
  public static void main(String args[]) throws Exception {
    // create dummy harness and invoke with dummy arguments
      (new Antlr(null, null)).run(null, "");
  }
}
