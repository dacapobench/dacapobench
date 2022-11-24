package org.dacapo.analysis;

import java.util.HashMap;

public class BCCAnalysis {

    public static String[] mnemonic = {"nop", "aconst_null", "iconst_m1", "iconst_0", "iconst_1", "iconst_2", "iconst_3", "iconst_4", "iconst_5", "lconst_0", "lconst_1", "fconst_0", "fconst_1", "fconst_2", "dconst_0", "dconst_1", "bipush", "sipush", "ldc", "ldc_w", "ldc2_w", "iload", "lload", "fload", "dload", "aload", "iload_0", "iload_1", "iload_2", "iload_3", "lload_0", "lload_1", "lload_2", "lload_3", "fload_0", "fload_1", "fload_2", "fload_3", "dload_0", "dload_1", "dload_2", "dload_3", "aload_0", "aload_1", "aload_2", "aload_3", "iaload", "laload", "faload", "daload", "aaload", "baload", "caload", "saload", "istore", "lstore", "fstore", "dstore", "astore", "istore_0", "istore_1", "istore_2", "istore_3", "lstore_0", "lstore_1", "lstore_2", "lstore_3", "fstore_0", "fstore_1", "fstore_2", "fstore_3", "dstore_0", "dstore_1", "dstore_2", "dstore_3", "astore_0", "astore_1", "astore_2", "astore_3", "iastore", "lastore", "fastore", "dastore", "aastore", "bastore", "castore", "sastore", "pop", "pop2", "dup", "dup_x1", "dup_x2", "dup2", "dup2_x1", "dup2_x2", "swap", "iadd", "ladd", "fadd", "dadd", "isub", "lsub", "fsub", "dsub", "imul", "lmul", "fmul", "dmul", "idiv", "ldiv", "fdiv", "ddiv", "irem", "lrem", "frem", "drem", "ineg", "lneg", "fneg", "dneg", "ishl", "lshl", "ishr", "lshr", "iushr", "lushr", "iand", "land", "ior", "lor", "ixor", "lxor", "iinc", "i2l", "i2f", "i2d", "l2i", "l2f", "l2d", "f2i", "f2l", "f2d", "d2i", "d2l", "d2f", "i2b", "i2c", "i2s", "lcmp", "fcmpl", "fcmpg", "dcmpl", "dcmpg", "ifeq", "ifne", "iflt", "ifge", "ifgt", "ifle", "if_icmpeq", "if_icmpne", "if_icmplt", "if_icmpge", "if_icmpgt", "if_icmple", "if_acmpeq", "if_acmpne", "goto", "jsr", "ret", "tableswitch", "lookupswitch", "ireturn", "lreturn", "freturn", "dreturn", "areturn", "return", "getstatic", "putstatic", "getfield", "putfield", "invokevirtual", "invokespecial", "invokestatic", "invokeinterface", "invokedynamic", "new", "newarray", "anewarray", "arraylength", "athrow", "checkcast", "instanceof", "monitorenter", "monitorexit", "wide", "multianewarray", "ifnull", "ifnonnull", "goto_w", "jsr_w", "breakpoint", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "impdep1", "impdep2"};

    /* Benchmark Callback hooks */
    public static void iterationStart(boolean warmup) {
        reset();
    }

    public static void iterationStop(boolean warmup) {
    }

    private static boolean check() {
        int sum = 0;
        for (int id : executed.keySet()) {
            sum += executed.get(id);
        }
        if (sum != bytecodesExecuted) {
            System.err.println("WARNING: inconsistent count of bytecodes executed ("+sum+" != "+bytecodesExecuted+")");
            return false;
        }
        return true;
    }

    public static void benchmarkComplete(boolean valid) {
        if (check()) {
            System.out.println("transformed-classes: "+ classesTransformed);
            System.out.println("transformed-bytecodes: "+ bytecodesTransformed);
            System.out.println("executed-bytecodes: "+ bytecodesExecuted);
            System.out.println("executed-bytecodes-unique: "+ executed.size());

            System.out.print("opcodes: {");
            boolean start = true;
            for (int i = 0; i < 255; i++) {
                if (opcodes[i] != 0) {
                    System.out.print((start ? " " : ", ")+mnemonic[i]+": "+opcodes[i]);
                    start = false;
                }
            }
            System.out.println(" }");
        }
    }

    public static long classesTransformed= 0;
    public static long bytecodesTransformed = 0;
    public static long bytecodesExecuted = 0;

    public static HashMap<Integer, Integer> executed = new HashMap();
    public static int[] opcodes = new int[255];


    private static void reset() {
        bytecodesExecuted = 0;
        executed = new HashMap();
        for (int i = 0; i < 255; i++) {
            opcodes[i] = 0;
        }
    }

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
        opcodes[opcode]++;
        if (executed.containsKey(id)) {
            int old = executed.get(id);
            executed.put(id, ++old);
        } else {
            executed.put(id, 1);
        }
        bytecodesExecuted++;
    }
}
