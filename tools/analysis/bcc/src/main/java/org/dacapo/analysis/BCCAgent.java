package org.dacapo.analysis;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.net.URI;
import java.io.File;
import java.util.jar.JarFile;

import org.dacapo.analysis.BytecodeCallback;


public class BCCAgent {

    private static volatile boolean canRewriteBootstrap;

    static boolean canRewriteClass(String className, ClassLoader loader) {
        // There are two conditions under which we don't rewrite:
        //  1. If className was loaded by the bootstrap class loader and
        //  the agent wasn't (in which case the class being rewritten
        //  won't be able to call agent methods).
        //  2. If it is java.lang.ThreadLocal, which can't be rewritten because the
        //  JVM depends on its structure.
        if (((loader == null) && !canRewriteBootstrap)
            || className.startsWith("java/lang/ThreadLocal")) {
          return false;
        }
        // third_party/java/webwork/*/ognl.jar contains bad class files.  Ugh.
        if (className.startsWith("ognl/")) {
          return false;
        }

        if (className.startsWith("org/dacapo/analysis")) {
          return false;
        }

        return true;
      }

    public static void premain(String agentArgs, Instrumentation inst) {
        // Force eager class loading here.  The instrumenter relies on these classes.  If we load them
        // for the first time during instrumentation, the instrumenter will try to rewrite them.  But
        // the instrumenter needs these classes to run, so it will try to load them during that rewrite
        // pass.  This results in a ClassCircularityError.
        try {
            Class.forName("sun.security.provider.PolicyFile");
            Class.forName("java.util.ResourceBundle");
            Class.forName("java.util.Date");
        } catch (Throwable t) {
            // NOP
        }

        if (!inst.isRetransformClassesSupported()) {
            System.err.println("Some JDK classes are already loaded and will not be instrumented.");
        }



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
}
