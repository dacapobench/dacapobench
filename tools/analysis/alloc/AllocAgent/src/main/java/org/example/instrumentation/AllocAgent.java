package org.example.instrumentation;

import org.example.AllocTransformer;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;

public class AllocAgent {
    public static void premain(String agentArgs, Instrumentation inst) {
        final ClassFileTransformer transformer = new AllocTransformer();

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
        inst.addTransformer(transformer, true);
    }
}
