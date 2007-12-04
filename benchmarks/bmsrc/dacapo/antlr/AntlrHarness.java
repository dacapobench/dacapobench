package dacapo.antlr;

import java.io.File;
import java.util.Vector;

import dacapo.Benchmark;
import dacapo.parser.Config;
import antlr.Tool;

/**
 * Benchmark harness for the Antlr benchmark
 *
 * @author Robin Garner
 * @author Steve Blackburn
 * @date $Date: 2007-12-04 14:54:00 +1100 (Tue, 04 Dec 2007) $
 * @id $Id: AntlrHarness.java 310 2007-12-04 03:54:00Z steveb-oss $
 *
 */
public class AntlrHarness extends Benchmark {

  public AntlrHarness(Config config, File scratch) throws Exception {
    super(config,scratch);
  }

  protected void prepare() throws Exception {
    super.prepare();
    copyFileTo(new File(scratch,"antlr/CommonTokenTypes.txt"),scratch);
  }

  /**
   * After each iteration, delete the output files
   */
  public void postIteration(String size) throws Exception {
    super.postIteration(size);
    if (!isPreserve())
      deleteTree(new File(scratch,"antlr/output"));
  }

  public void iterate(String size) {
    String[] args = preprocessArgs(size);
    int firstGrammarIndex = 0;
    int nIterations = 1;
    String[] newArgs = null;
    Vector<String> v = new Vector<String>(args.length);
    for (int i=0; i < args.length; i++) {
      if (args[i].equals("-grammars")) {
        firstGrammarIndex = i+1;
        newArgs = new String[v.size()+1];
        for (int j=0; j < v.size(); j++) {
          newArgs[j] = (String)v.elementAt(j);
        }
        break;
      } else if (args[i].equals("-iterations")) {
        nIterations = Integer.parseInt(args[++i]);
      } else {
        v.addElement(args[i]);
      }
    }

    for (int iteration=0; iteration < nIterations; iteration++) {
      for (int iGrammar=firstGrammarIndex;
           iGrammar < args.length;
           iGrammar++) {
        String grammarFile = (new File(scratch,args[iGrammar])).getPath();
        newArgs[newArgs.length-1] = grammarFile;
        System.out.println(args[iGrammar]);
        Tool.main(newArgs);
      }
    }
  }
}
