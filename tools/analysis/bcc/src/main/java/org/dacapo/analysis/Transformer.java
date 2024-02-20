package org.dacapo.analysis;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.UnmodifiableClassException;
import java.security.ProtectionDomain;

import org.dacapo.analysis.BytecodeCallback;

public class Transformer implements ClassFileTransformer {

    boolean booted = false;
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        // if (classBeingRedefined == null)
        //     return null;
        if (!BCCAgent.canRewriteClass(className, loader)) {
            BytecodeCallback.classSkipped(className);
            throw new RuntimeException(new UnmodifiableClassException("cannot instrument " + className));
        }
        // for (ClassLoader curLoader = loader; ; curLoader = curLoader.getParent()) {
        //     if (curLoader == null) {
        //         String suffix = loader != null ? "for non-null loader " + loader : "with null loader";
        //         BCCAnalysis.classSkipped("Skipped '" + className + "' " + suffix);
        //         return null;
        //      } else if (curLoader == BCCAnalysis.class.getClassLoader())
        //          break;
        // }
        BytecodeCallback.classTransformed(className);
        return new Rewriter(className, classfileBuffer).rewrite();
    }
}
