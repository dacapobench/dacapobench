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
 * @date $Date: 2008-07-26 11:23:30 +1000 (Sat, 26 Jul 2008) $
 * @id $Id: Antlr.java 379 2008-07-26 01:23:30Z steveb-oss $
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
}
