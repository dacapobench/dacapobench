package org.dacapo.analysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;


public class BCCAnalysis {

    public static long classesTransformed= 0;
    public static Long bytecodesTransformed = new Long(0);
    public static long bytecodesExecuted = 0;
    public static long callsExecuted = 0;

    public static HashMap<Integer, Long> executed = new HashMap();
    public static HashMap<Integer, Long> called = new HashMap();
    public static List<String> skipped = new ArrayList();

    public static long[] opcodes = new long[255];


    public static String[] mnemonic = {"nop", "aconst_null", "iconst_m1", "iconst_0", "iconst_1", "iconst_2", "iconst_3", "iconst_4", "iconst_5", "lconst_0", "lconst_1", "fconst_0", "fconst_1", "fconst_2", "dconst_0", "dconst_1", "bipush", "sipush", "ldc", "ldc_w", "ldc2_w", "iload", "lload", "fload", "dload", "aload", "iload_0", "iload_1", "iload_2", "iload_3", "lload_0", "lload_1", "lload_2", "lload_3", "fload_0", "fload_1", "fload_2", "fload_3", "dload_0", "dload_1", "dload_2", "dload_3", "aload_0", "aload_1", "aload_2", "aload_3", "iaload", "laload", "faload", "daload", "aaload", "baload", "caload", "saload", "istore", "lstore", "fstore", "dstore", "astore", "istore_0", "istore_1", "istore_2", "istore_3", "lstore_0", "lstore_1", "lstore_2", "lstore_3", "fstore_0", "fstore_1", "fstore_2", "fstore_3", "dstore_0", "dstore_1", "dstore_2", "dstore_3", "astore_0", "astore_1", "astore_2", "astore_3", "iastore", "lastore", "fastore", "dastore", "aastore", "bastore", "castore", "sastore", "pop", "pop2", "dup", "dup_x1", "dup_x2", "dup2", "dup2_x1", "dup2_x2", "swap", "iadd", "ladd", "fadd", "dadd", "isub", "lsub", "fsub", "dsub", "imul", "lmul", "fmul", "dmul", "idiv", "ldiv", "fdiv", "ddiv", "irem", "lrem", "frem", "drem", "ineg", "lneg", "fneg", "dneg", "ishl", "lshl", "ishr", "lshr", "iushr", "lushr", "iand", "land", "ior", "lor", "ixor", "lxor", "iinc", "i2l", "i2f", "i2d", "l2i", "l2f", "l2d", "f2i", "f2l", "f2d", "d2i", "d2l", "d2f", "i2b", "i2c", "i2s", "lcmp", "fcmpl", "fcmpg", "dcmpl", "dcmpg", "ifeq", "ifne", "iflt", "ifge", "ifgt", "ifle", "if_icmpeq", "if_icmpne", "if_icmplt", "if_icmpge", "if_icmpgt", "if_icmple", "if_acmpeq", "if_acmpne", "goto", "jsr", "ret", "tableswitch", "lookupswitch", "ireturn", "lreturn", "freturn", "dreturn", "areturn", "return", "getstatic", "putstatic", "getfield", "putfield", "invokevirtual", "invokespecial", "invokestatic", "invokeinterface", "invokedynamic", "new", "newarray", "anewarray", "arraylength", "athrow", "checkcast", "instanceof", "monitorenter", "monitorexit", "wide", "multianewarray", "ifnull", "ifnonnull", "goto_w", "jsr_w", "breakpoint", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "impdep1", "impdep2"};

    /* Benchmark Callback hooks */
    public static void iterationStart(boolean warmup) {
        reset();
    }

    public static void iterationStop(boolean warmup) {
    }

    private static boolean check() {
        long esum = 0;
        for (int id : executed.keySet()) {
            esum += executed.get(id);
        }
        long osum = 0;
        for (int i = 0; i < opcodes.length; i++) {
            osum += opcodes[i];
        }
        if (esum != bytecodesExecuted || osum != bytecodesExecuted) {
            System.err.println("WARNING: inconsistent count of bytecodes executed ("+esum+", "+osum+", "+bytecodesExecuted+")");
            return false;
        }
        return true;
    }

    public static void benchmarkComplete(boolean valid) {
        synchronized (bytecodesTransformed) {
            check();

            List<Long> bytecodefreq = new ArrayList<Long>(executed.values());
            Collections.sort(bytecodefreq);
            int uniq = bytecodefreq.size();
            long p90 = bytecodefreq.get((uniq-1)-(uniq/10));
            long p99 = bytecodefreq.get((uniq-1)-(uniq/100));
            long p999 = bytecodefreq.get((uniq-1)-(uniq/1000));
            long p9999 = bytecodefreq.get((uniq-1)-(uniq/10000));

            System.out.println("transformed-classes: "+ classesTransformed);
            System.out.println("transformed-bytecodes: "+ bytecodesTransformed);
            System.out.println("executed-bytecodes: "+ bytecodesExecuted);
            System.out.println("executed-bytecodes-unique: "+ executed.size());
            System.out.println("executed-bytecodes-p90: "+ p90);
            System.out.println("executed-bytecodes-p99: "+ p99);
            System.out.println("executed-bytecodes-p999: "+ p999);
            System.out.println("executed-bytecodes-p9999: "+ p9999);
            System.out.println("executed-calls: "+ callsExecuted);
            System.out.println("executed-calls-unique: "+ called.size());

            System.out.print("opcodes: {");
            boolean start = true;
            for (int i = 0; i < 255; i++) {
                if (opcodes[i] != 0) {
                    System.out.print("\n  "+mnemonic[i]+": "+opcodes[i]);
                    start = false;
                }
            }
            System.out.println("\n}");

            for (String msg : skipped) {
                System.out.println(msg);
            }
        }
    }

    private static void reset() {
        synchronized (bytecodesTransformed) {
            bytecodesExecuted = 0;
            callsExecuted = 0;
            executed = new HashMap();
            called = new HashMap();
            for (int i = 0; i < 255; i++) {
                opcodes[i] = 0;
            }
        }
    }

    static void preMainClassLoaded(Class clazz) {
    }
  
    static void classTransformed(String className) {
        classesTransformed++;
    }

    public static int bytecodeTransformed() {
        synchronized (bytecodesTransformed) {
            bytecodesTransformed++;
        }
        if (bytecodesTransformed >= Integer.MAX_VALUE) {
            System.err.println("WARNING: overflow on bytecode ID");
            return Integer.MAX_VALUE;
        } else 
            return bytecodesTransformed.intValue();
    }

    public static void bytecodeExecuted(int opcode, int id) {
        synchronized (bytecodesTransformed) {
            opcodes[opcode]++;

            /* calls */
            if (opcode >= 182 && opcode <= 186) { // invokevirtual, invokespecial, invokestatic, invokeinterface, invokedynamic
                callsExecuted++;
                if (called.containsKey(id)) {
                    long old = called.get(id);
                    called.put(id, ++old);
                } else {
                    called.put(id, 1L);
                }
            }

            if (executed.containsKey(id)) {
                long old = executed.get(id);
                executed.put(id, ++old);
            } else {
                executed.put(id, 1L);
            }
            bytecodesExecuted++;
        }
    }

    public static void classSkipped(String message) {
        synchronized (bytecodesTransformed) {
            skipped.add(message);
        }
    }
}
