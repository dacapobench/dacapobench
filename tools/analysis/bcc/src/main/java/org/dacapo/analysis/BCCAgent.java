package org.dacapo.analysis;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;

public class BCCAgent {
    public static long f;
    public static void foo() {
        f++;
    }
    public static void premain(String agentArgs, Instrumentation inst) {
        final ClassFileTransformer transformer = new Transformer();

        Class[] classes = inst.getAllLoadedClasses();
        try {
            for (Class clazz : classes) {
                BCCAnalysis.preMainClassLoaded(clazz);
                if (clazz.getName().startsWith("java.lang.Object")) {
                    inst.retransformClasses(clazz);
                }
            }
        } catch (UnmodifiableClassException e) {
            throw new RuntimeException(e);
        }
        inst.addTransformer(transformer, true);
    }
}
