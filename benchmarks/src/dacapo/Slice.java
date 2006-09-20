package dacapo;

import java.io.*;

public class Slice {
  
  private static void extract(File inFile, File outFile, int offset, int length) throws IOException {
    BufferedReader in = new BufferedReader(new FileReader(inFile));
    PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(outFile)));
    int lineNo = 0;
    String line;
    while ((line = in.readLine()) != null && lineNo < offset + length) {
      if (offset <= lineNo && lineNo < offset + length)
        out.println(line);
      lineNo++;
    }
    in.close();
    out.close();
  }

  public static void slice(File file, File dir, int fraction) throws IOException {
    int length = Benchmark.lineCount(file);
    int lines = (length)/fraction;  // round up
    
    /* iterate over the length of the output file */
    for (int pass=1; pass <= fraction; pass++) {
      int chunk = pass * lines; // Length of the sub-file at this pass
      
      /* Iterate over the offset into the initial file */
      for (int offset = 0, n=0; offset <= length - chunk; offset += lines, n++) {
        File outFile = new File(dir,file.getName()+"."+fraction+"."+pass+"."+n);
        extract(file,outFile,offset,chunk);
      }
    }
  }
  
  /**
   * @param args
   */
  public static void main(String[] args) throws IOException {
    // TODO Auto-generated method stub
    String file = args[0];
    String targetDir = args[1];
    int fraction = Integer.parseInt(args[2]);
    
    slice(new File(file),new File(targetDir),fraction);
  }

}
