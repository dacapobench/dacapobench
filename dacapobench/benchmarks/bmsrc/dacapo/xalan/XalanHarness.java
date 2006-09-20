package dacapo.xalan;

import java.io.*;

import dacapo.Benchmark;
import dacapo.parser.Config;

public class XalanHarness extends Benchmark {
  public XalanHarness(Config config, File scratch) throws Exception {
    super(config, scratch);
  }

  public void iterate(String size) throws Exception {
    String[] harnessArgs = config.getArgs(size);
    int nRuns = Integer.parseInt(harnessArgs[0]);
    int iWorkload = Integer.parseInt(harnessArgs[1]);
    String in=null, xsl=null;

    if(iWorkload == 1){
      in = "xalan/xslbenchdream.xml";
      xsl = "xalan/xslbench3.xsl";
    } else if(iWorkload == 2){
      in = "xalan/william.xml";
      xsl = "xalan/william.xsl";
    }
    for(int iRun=0; iRun<nRuns; iRun++){
      File outFile = new File(scratch,"xalan.out."+iRun);
      String[] cmdArgs = new String[] {
        "-IN", fileInScratch(in),
        "-XSL", fileInScratch(xsl),
        "-OUT", outFile.getPath(),
        "-EDUMP"};
      org.apache.xalan.xslt.Process.main(cmdArgs);
      System.out.println("Run " + iRun + " completed");
    }
    System.out.println("Normal completion.");
  }
}
