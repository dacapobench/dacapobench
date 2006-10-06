package dacapo.eclipse;


import java.io.File;
import java.io.IOException;

import org.eclipse.core.runtime.IPlatformRunnable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.internal.launching.StandardVMType;

public class HarnessRunner implements IPlatformRunnable { 
  /**
   * The main entrypoint into this class.
   * 
   * @param args The commandline arguments for running this class
   * @see org.eclipse.core.runtime.IPlatformRunnable
   */
  public Object run(Object args) throws Exception {
    if (validJavaHome()) {
      try {
        EclipseTests.runtests((String[]) args);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    return null; 
  }
  
  private boolean validJavaHome() {
    File javaHome = null; 
    IStatus status = null;  
    try {
      javaHome = new File (System.getProperty("java.home")).getCanonicalFile();
    } catch (IOException e) {
      System.err.println("Error opening JAVA_HOME: "+System.getProperty("java.home"));  
      return false;
    }
    status = new StandardVMType().validateInstallLocation(javaHome);
    if (status != null && status.getSeverity() == IStatus.OK)
      return true;
    else {
      System.err.println("Eclipse cannot validate the JAVA_HOME: "+System.getProperty("java.home"));
      System.err.println("\tPlease set the eclipse.java.home system property to point to");
      System.err.println("\ta valid JRE.  This JRE is used by eclipse to compile against.");
      System.err.println("\t(e.g. run dacapo with \"-Declipse.java.home=/usr/lib/j2se/1.4\")");
      return false;
    }
  }

}

