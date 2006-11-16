package com.ibm.jikesrvm.memorymanagers.mminterface;

/**
 * As well as being a compilation stub, this provides backward compatibility 
 * to earlier releases of JikesRVM.  On a JikesRVM build newer than Nov 2006, 
 * the stub should never be called, as the class built in to the VM will take 
 * precedence.  On an earlier build this stub should invoke the VM's version
 * of the class, providing backward compatibility.
 * 
 * NOTE: the 'active' flag isn't thread safe, but it should provide
 * adequate protection against unbounded recursion.
 * 
 * @author Robin Garner
 * @date $Date:$
 * @id $Id:$
 *
 */
public class MM_Interface {
  private static boolean active = false;
  public static void harnessBegin() { 
    if (active)
      warn(); 
    else {
      active = true;
      com.ibm.JikesRVM.memoryManagers.mmInterface.MM_Interface.harnessBegin();
      active = false;
    }
  }
  public static void harnessEnd() {
    if (active)
      warn(); 
    else {
      active = true;
      com.ibm.JikesRVM.memoryManagers.mmInterface.MM_Interface.harnessEnd();
      active = false;
    }
  }
  private static void warn() {
    System.err.println("WARNING: stub com.ibm.JikesRVM.memoryManagers.mmInterface.MM_Interface called");
  }
}
