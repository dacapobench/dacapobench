package dacapo.antlr;

import java.io.File;
import java.util.Vector;

import dacapo.Benchmark;
import dacapo.parser.Config;

/**
 * Benchmark harness for the Antlr benchmark
 *
 * @author Robin Garner
 * @author Steve Blackburn
 * @date $Date: 2007-12-07 12:00:41 +1100 (Fri, 07 Dec 2007) $
 * @id $Id: AntlrHarness.java 316 2007-12-07 01:00:41Z rgarner $
 *
 */
public class AntlrHarness extends Benchmark {

  private String[] args;
  private int firstGrammarIndex;
  private int iterations;
  private String[] antlrToolArgs;
  
  public AntlrHarness(Config config, File scratch) throws Exception {
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
