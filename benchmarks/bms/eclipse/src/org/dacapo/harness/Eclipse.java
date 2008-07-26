package org.dacapo.harness;

import java.io.File;

//import org.eclipse.core.runtime.adaptor.EclipseStarter;
import java.lang.reflect.Method;
import org.dacapo.harness.Benchmark;
import org.dacapo.parser.Config;

public class Eclipse extends Benchmark {
  
  protected static final String ECLIPSE_SOURCE_ZIP = "eclipse/plugins/org.eclipse.jdt.core.tests.performance_3.1.2/full-source-R3_0.zip";
  static final String wsDirectory = "workspace";
  static String oldJavaHome = null;
  
  private final Method isRunning;
  private final Method run;
  private final Method shutdown;
  
  public Eclipse(Config config, File scratch) throws Exception {
    super(config, scratch);
    Class<?> clazz = Class.forName("org.eclipse.core.runtime.adaptor.EclipseStarter", true, loader);
    this.method = clazz.getMethod("startup", String[].class, Runnable.class);
    this.isRunning = clazz.getMethod("isRunning");
    this.run = clazz.getMethod("run", Object.class);
    this.shutdown = clazz.getMethod("shutdown");
  }
  
  public void preIteration(String size) throws Exception {
    super.preIteration(size);
    createWorkspace();
    //if (!EclipseStarter.isRunning()) {
    if (!((Boolean) isRunning.invoke(null, (Object[]) null)).booleanValue()) {
      startup(size);
    }
    setJavaHomeIfRequired();
  }
  
  public void iterate(String size) throws Exception {
    try {
      run.invoke(null, new Object[] { null });
    } catch (Exception e) {
      e.printStackTrace();
    } 
  } 
  
  public void postIteration(String size) throws Exception {
    super.postIteration(size);
    restoreJavaHomeIfRequired();
    if (!isPreserve())
      deleteTree(new File(scratch,wsDirectory));
  }
  
  public void cleanup() {
    try {
      shutdown.invoke(null, (Object[]) null);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  
  private void createWorkspace() throws Exception {
    File wsdir = new File(scratch, wsDirectory);
    if (wsdir.exists())
      deleteTree(new File(scratch,wsDirectory));
    wsdir.mkdir();
    unpackZipFile(fileInScratch(ECLIPSE_SOURCE_ZIP),wsdir);
  }

  private void startup(String size) {
    try {
      System.setProperty("osgi.os","linux");
      System.setProperty("osgi.ws","gtk");
      System.setProperty("osgi.arch","x86");
      System.setProperty("osgi.install.area",  "file:"+fileInScratch("eclipse/"));
      System.setProperty("osgi.noShutdown", "true");
      System.setProperty("osgi.framework","file:"+fileInScratch("eclipse/plugins/org.eclipse.osgi_3.1.2.jar"));
      System.setProperty("eclipse.java.home", fileInScratch("dummyjre"));
      /*
       * Hard-wire some properties that could otherwise be overriden in the
       * environment. 
       */
      System.setProperty("javax.xml.parsers.DocumentBuilderFactory", 
                "org.apache.xerces.jaxp.DocumentBuilderFactoryImpl");
      
      setJavaHomeIfRequired();
      
      String[] pluginArgs = config.getArgs(size);
      String[] args = new String[4 + pluginArgs.length];
      args[0] = "-data";
      args[1] = fileInScratch("workspace");
      args[2] = "-application";
      args[3] = "dacapo.eclipse.dacapoHarness";
      for (int i = 0; i < pluginArgs.length; i++)
        args[4+i] = pluginArgs[i];
//      EclipseStarter.startup(args, null);
      method.invoke(null, new Object[] {args, null});
    } catch (Exception e) {
        e.printStackTrace();
    }
  }

  private void setJavaHomeIfRequired() {
    String eclipseJavaHome = System.getProperty("eclipse.java.home");
    if (eclipseJavaHome != null) {
      oldJavaHome = System.getProperty("java.home");
      System.setProperty("java.home", eclipseJavaHome);
    }
  }
  
  private void restoreJavaHomeIfRequired() {
    if (oldJavaHome != null)
      System.setProperty("java.home", oldJavaHome);
  }
}
