package org.dacapo.analysis;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.ListIterator;

public class BCCAnalysis {

  /* Benchmark Callback hooks */
  static void iterationStart(boolean warmup) {

  }
  static void iterationStop(boolean warmup) {

  }

  static void benchmarkComplete(boolean valid) {
    System.err.println("Loaded "+classesLoaded+" classes, executed "+bytecodesExecuted+" bytecodes");
  }

  public static int classesLoaded = 0;
  public static long bytecodesExecuted = 0;


  static void preMainClassLoaded(Class clazz) {
  }

  static void classLoaded(String className) {
    classesLoaded++;
  }

  public static void bytecodeExecuted() {
    bytecodesExecuted++;
  }

  static void bytecodeExecuted(int opcode, int id) {
    bytecodesExecuted++;
  }
}
