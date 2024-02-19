package org.dacapo.analysis;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.net.URI;
import java.io.File;
import java.util.jar.JarFile;

public class BCCAgent {
    public static void premain(String agentArgs, Instrumentation inst) {
//        The following is not needed as we are not interested classes that have been loaded
//        before this agent
//        Class[] classes = inst.getAllLoadedClasses();
//        try {
//            for (Class clazz : classes) {
//                System.out.println(clazz.getName());
//                if (clazz.getName().contains("Test")) {
//                    inst.retransformClasses(clazz);
//                }
//            }
//        } catch (UnmodifiableClassException e) {
//            throw new RuntimeException(e);
//        }
        final ClassFileTransformer transformer = new Transformer();
        inst.addTransformer(transformer, true);
    }

    public static void agentmain(String agentArgs, Instrumentation inst) {
        final ClassFileTransformer transformer = new Transformer();
        inst.addTransformer(transformer, true);
    }

}
