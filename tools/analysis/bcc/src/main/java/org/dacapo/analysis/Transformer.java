package org.dacapo.analysis;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

public class Transformer implements ClassFileTransformer {

    boolean booted = false;
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {

        String dontskip = System.getProperty("skip.exception");
    
        boolean ok = (dontskip != null && className.startsWith(dontskip));

        if (loader != null &&
          !loader.toString().startsWith("jdk.internal.loader.ClassLoaders$PlatformClassLoader") &&
          !loader.toString().startsWith("jdk.internal.reflect.DelegatingClassLoader") &&
          !className.startsWith("org/dacapo/analysis")) {
            BCCAnalysis.classTransformed(className);
            return new Rewriter(className, classfileBuffer).rewrite();
        } else {
    //       System.err.println("skipped class: " + loader + " " + className);
        }
        return null;
    }
}
