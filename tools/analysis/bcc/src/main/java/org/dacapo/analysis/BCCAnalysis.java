package org.dacapo.analysis;

import java.util.HashMap;

public class BCCAnalysis {

    /* Benchmark Callback hooks */
    public static void iterationStart(boolean warmup) {

    }
    public static void iterationStop(boolean warmup) {

    }

    public static void benchmarkComplete(boolean valid) {
        System.err.println("Transformed "+        classesTransformed++
        +" classes,  "+bytecodesTransformed+" bytecodes, executed "+bytecodesExecuted+" bytecodes, "+executed.size()+" unique");
    }

    public static long classesTransformed= 0;
    public static long bytecodesTransformed = 0;
    public static long bytecodesExecuted = 0;

    public static HashMap<Integer, Integer> executed = new HashMap();

    static void preMainClassLoaded(Class clazz) {
    }
  
    static void classTransformed(String className) {
        classesTransformed++;
    }

    public static void bytecodeTransformed() {
        bytecodesTransformed++;
      }

      
    public static void bytecodeExecuted() {
      //  bytecodesExecuted++;
    }

    public static void bytecodeExecuted(int opcode) {
      //  bytecodesExecuted++;
    }

    public static void bytecodeExecuted(int opcode, int id) {
        if (executed.containsKey(id)) {
            int old = executed.get(id);
            executed.put(id, ++old);
        } else {
            executed.put(id, 1);
        }
        bytecodesExecuted++;
    }
}
