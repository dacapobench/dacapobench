package dacapo.eclipse;

import java.io.File;
import java.io.IOException;
import java.net.*;
import java.io.*;
import java.lang.reflect.Method;

import org.eclipse.core.runtime.adaptor.EclipseStarter;

import dacapo.Benchmark;
import dacapo.parser.Config;

public class EclipseHarness extends Benchmark {
  
  static final String wsDirectory = "workspace";
  static String oldJavaHome = null;
  
  public EclipseHarness(Config config, File scratch) throws Exception {
    super(config, scratch);
  }
  
  public void preIteration(String size) throws Exception {
    super.preIteration(size);
    File wsdir = new File(scratch, wsDirectory);
    wsdir.mkdir();
    unpackZipFile(fileInScratch("eclipse/plugins/org.eclipse.jdt.core.tests.performance_3.1.2/full-source-R3_0.zip"),wsdir);
  }
  
  public void iterate(String size) throws Exception {
    try {
      if (!EclipseStarter.isRunning())
        startup(size);
      setJavaHomeIfRequired();
      EclipseStarter.run(null);
    } catch (Exception e) {
      e.printStackTrace();
    } 
  } 
  
  public void postIteration(String size) throws Exception {
    super.postIteration(size);
    restoreJavaHomeIfRequired();
    if (!preserve)
      deleteTree(new File(scratch,wsDirectory));
  }
  
  public void cleanup() {
    try {
      EclipseStarter.shutdown();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void startup(String size) {
    try {
      System.setProperty("osgi.os","linux");
      System.setProperty("osgi.ws","gtk");
      System.setProperty("osgi.arch","x86");
      System.setProperty("osgi.install.area",  "file:"+fileInScratch("eclipse/"));
      System.setProperty("osgi.noShutdown", "true");
      System.setProperty("osgi.framework","file:"+fileInScratch("eclipse/plugins/org.eclipse.osgi_3.1.2.jar"));
      setJavaHomeIfRequired();
      String[] pluginArgs = config.getArgs(size);
      String[] args = new String[4 + pluginArgs.length];
      args[0] = "-data";
      args[1] = fileInScratch("workspace");
      args[2] = "-application";
      args[3] = "dacapo.eclipse.dacapoHarness";
      for (int i = 0; i < pluginArgs.length; i++)
        args[4+i] = pluginArgs[i];
      EclipseStarter.startup(args, null);
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
