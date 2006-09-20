package dacapo.parser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.Vector;

public class Config {

  class OutputFile {
    String name;
    String digest = null;
    boolean keep = false;
    int lines = -1;
    long bytes = -1;
    
    OutputFile(String name) {
      this.name = name;
    }
    boolean hasDigest() { return digest != null; }
    boolean hasLines() { return lines != -1; }
    boolean hasBytes() { return bytes != -1; }
  }
  
  class Size {
    final String name;
    final String[] args;
    HashMap outputFiles = new LinkedHashMap(20);
    
    public Size(String name, Vector args) {
      this.args = (String[])args.toArray(new String[0]);
      this.name = name;
    }
    void addOutputFile(String file) {
      outputFiles.put(file,new OutputFile(file));
    }
    OutputFile getOutputFile(String file) {
      return (OutputFile)outputFiles.get(file);
    }
  }
  
  public final String name;
  public String className = null;
  public String methodName = null;
  
  HashMap sizes = new HashMap(3);
  HashMap desc = new HashMap(6);
  
  Config(String name) {
    this.name = name;
  }
  
  void setClass(String klass) { this.className = klass; }
  void setMethod(String method) { this.methodName = method; }
  
  void addSize(String name, Vector args) {
    sizes.put(name,new Size(name,args));
  }
  
  private Size getSize(String size) {
    Size s = (Size)sizes.get(size);
    if (s == null) {
      System.err.println("No such size: \""+size+"\" in this configuration");
    }
    return s; 
  }
  
  void addDesc(String element, String description) {
    desc.put(element,description);
  }
  
  /*************************************************************************************
   * Output files
   */
  
  void addOutputFile(String size, String file) {
    getSize(size).addOutputFile(file);
  }
  
  void setDigest(String size, String file, String digest) {
    getSize(size).getOutputFile(file).digest = digest;
  }
  
  void setLines(String size, String file, int lines) {
    getSize(size).getOutputFile(file).lines = lines;
  }
  
  void setBytes(String size, String file, long bytes) {
    getSize(size).getOutputFile(file).bytes = bytes;
  }
  
  void setKeep(String size, String file) {
    getSize(size).getOutputFile(file).keep = true;
  }
  
  /**
   * Parse a config file
   * 
   * @param file
   * @return
   */
  public static Config parse(String file) { return parse(new File(file)); }
  
  public static Config parse(File file) {
    try {
      return parse(new FileInputStream(file));
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
    return null;
  }
  
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
  
  /*
   * Getter methods
   */
  public String[] getArgs(String config) {
    Size s = getSize(config);
    return (String[])s.args.clone();
  }
  
  public Collection getSizes() {
    return sizes.keySet();
  }
  
  public Set getOutputs(String size) {
    return getSize(size).outputFiles.keySet();
  }
  
  public String getDigest(String size, String file) {
    Size s = getSize(size);
    return s.getOutputFile(file).digest;
  }
  
  public boolean hasDigest(String size, String file) {
    Size s = getSize(size);
    return s.getOutputFile(file).hasDigest();
  }
  
  public long getBytes(String size, String file) {
    Size s = getSize(size);
    return s.getOutputFile(file).bytes;
  }
  
  public boolean hasBytes(String size, String file) {
    Size s = getSize(size);
    return s.getOutputFile(file).hasBytes();
  }
  
  public int getLines(String size, String file) {
    Size s = getSize(size);
    return s.getOutputFile(file).lines;
  }
  
  public boolean hasLines(String size, String file) {
    Size s = getSize(size);
    return s.getOutputFile(file).hasLines();
  }
  
  public boolean isKept(String size, String file) {
    Size s = getSize(size);
    return s.getOutputFile(file).keep;
  }
  
  private String pad(String in, int length) {
    while (in.length() < length) 
      in += " ";
    return in;
  }
  
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
    if (methodName != null)
      str.print(" method "+methodName);
    str.println(";");
    
    for (Iterator i = getSizes().iterator(); i.hasNext(); ) {
      String size = (String)i.next();
      String[] args = getArgs(size);
      str.print("size "+size+" args \"");
      for (int j=0; j < args.length; j++) {
        if (j != 0)
          str.print(" ");
        str.print(args[j]);
      }
      str.println("\";");
      str.print("  outputs");
      for (Iterator v = getOutputs(size).iterator(); v.hasNext(); ) {
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

  /**
   * @param args
   */
  public static void main(String[] args) {
    // TODO Auto-generated method stub
    for (int i=0; i < args.length; i++) {
      parse(args[i]).print(System.out);
    }
  }

}
