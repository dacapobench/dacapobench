package org.dacapo.analysis;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

public class Transformer implements ClassFileTransformer {
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
      System.err.println(className);
      BCCAnalysis.classLoaded(className);
      byte[] result = new Rewriter(className, classfileBuffer).rewrite();
      return result;
    }
}
