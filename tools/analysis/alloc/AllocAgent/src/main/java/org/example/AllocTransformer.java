package org.example;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

public class AllocTransformer implements ClassFileTransformer {
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        // only instrument dacapo classes
        if (className.startsWith("org/dacapo")) {
            System.out.println("visit class: " + className);
            byte[] result = new AllocRewriter(className, classfileBuffer).rewrite();
            return result;
        }
        return null;
    }
}
