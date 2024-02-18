package org.dacapo.analysis;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

public class Transformer implements ClassFileTransformer {

    boolean booted = false;
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {

        // for (ClassLoader curLoader = loader; ; curLoader = curLoader.getParent()) {
        //     if (curLoader == null) {
        //         String suffix = loader != null ? "for non-null loader " + loader : "with null loader";
        //         BCCAnalysis.classSkipped("Skipped '" + className + "' " + suffix);
        //         return null;
        //      } else if (curLoader == BCCAnalysis.class.getClassLoader())
        //          break;
        // }
        BCCAnalysis.classTransformed(className);
        return new Rewriter(className, classfileBuffer).rewrite();
    }
}
