package dacapo.pmd;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import dacapo.Benchmark;
import dacapo.parser.Config;

import net.sourceforge.pmd.PMD;

public class PmdHarness extends Benchmark {

  public PmdHarness(Config config, File scratch) throws Exception {
    super(config, scratch);
  }

  public void iterate(String size) throws Exception {
    String[] args = config.getArgs(size);
    if (args[0].charAt(0) == '@')
      args[0] = collectFilesFromFile(fileInScratch(args[0].substring(1)));
    for (int i=2; i < args.length; i++) {
      if (args[i].charAt(0) != '-')
        args[i] = fileInScratch(args[i]);
    }
    PMD.main(args);
  }

  private String collectFilesFromFile(String inputFileName) {
    try {
      java.io.BufferedReader reader =
        new java.io.BufferedReader(new InputStreamReader(new FileInputStream(inputFileName)));
      
      List files = new ArrayList();
      
      for (String l=reader.readLine();
      l != null;
      l=reader.readLine()) {
        files.add(new File(scratch,l));
      }
      return commaSeparate(files);
    } catch (FileNotFoundException e) {
      throw new RuntimeException("File " + inputFileName + " error: "+e);
    } catch (java.io.IOException e) {
      throw new RuntimeException("File " + inputFileName + " error: "+e);
    }
    
  }
  
  private static String commaSeparate(List list) {
    String result = "";
    for (Iterator i = list.iterator(); i.hasNext(); ) {
      String s = ((File)i.next()).getPath();
      result += s;
      if (i.hasNext()) 
        result += ",";
    }
    return result;
  }

}
