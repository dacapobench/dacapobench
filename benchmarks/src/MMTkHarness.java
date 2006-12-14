/*
 * (C) Copyright Department of Computer Science,
 * Australian National University. 2005
 */
import java.lang.reflect.Method;

class MMTkHarness {
  private static final boolean verbose = false;
  private final String[] classes = new String[] {
        "org.mmtk.plan.Plan",                                          // Post 2.4.6
        "com.ibm.jikesrvm.memorymanagers.mminterface.MM_Interface",    // Post 2.4.5
        "com.ibm.JikesRVM.memoryManagers.mmInterface.MM_Interface",    // Pre 2.4.5
  };
    
  private Method beginMethod, endMethod;
    
  /**
   * Locate the class where the harness resides in various versions
   * of JikesRVM, and then retain Method references to the correct one.
   */
  MMTkHarness() {
    boolean found = false;
    for (int i=0; i < classes.length && !found; i++) {
      try {
        if (verbose) System.out.println("Trying "+classes[i]);
        Class harnessClass = Class.forName(classes[i]);
        beginMethod = harnessClass.getMethod("harnessBegin", null);        // Just check for the method
        endMethod = harnessClass.getMethod("harnessEnd", null);          // Just check for the method
        found = true;
      } catch (ClassNotFoundException c) {
        if (verbose)
          System.err.println("Could not locate "+classes[i]);
      } catch (SecurityException e) {
        if (verbose)
          System.err.println("harness method of "+classes[i]+" is not accessible");
      } catch (NoSuchMethodException e) {
        if (verbose)
          System.err.println("harness method of "+classes[i]+" not found");
      }
    }
    if (!found) {
      throw new RuntimeException("Unable to find MMTk Harness in any known parent class");
    }
  }
  
  void harnessBegin() {
    try {
      beginMethod.invoke(null, null);
    } catch (Exception e) {
      throw new RuntimeException("Error running MMTk harnessBegin",e);
    }
  }
  
  void harnessEnd() {
    try {
      endMethod.invoke(null, null);
    } catch (Exception e) {
      throw new RuntimeException("Error running MMTk harnessEnd",e);
    }
  }
}
  
