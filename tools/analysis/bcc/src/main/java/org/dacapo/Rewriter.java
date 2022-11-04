package org.dacapo;

import org.objectweb.asm.*;
import org.objectweb.asm.util.Printer;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceMethodVisitor;

import java.io.FileNotFoundException;
import java.io.PrintWriter;

import static org.objectweb.asm.Opcodes.*;

public class Rewriter {

    private final ClassReader classReader;
    private final ClassWriter classWriter;
    private final ClassVisitor classVisitor;

    private static int allocation_site = 0;

    public AllocRewriter(String className, byte[] classfileBuffer) {
        this.classReader = new ClassReader(classfileBuffer);
        this.classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_FRAMES);
        this.classVisitor = new ClassAdapter(className, classWriter);
    }

    public byte[] rewrite() {
        classReader.accept(classVisitor, 0);
        return classWriter.toByteArray();
    }

    private static class ClassAdapter extends ClassVisitor {

        private final String className;

        public ClassAdapter(String className, ClassVisitor classVisitor) {
            super(Opcodes.ASM9, classVisitor);
            this.className = className;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
            assert mv != null;
            return mv;
        }

    }
}
