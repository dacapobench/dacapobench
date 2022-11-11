package org.dacapo.analysis;

import org.objectweb.asm.*;
import org.objectweb.asm.util.TraceMethodVisitor;

import java.io.FileNotFoundException;
import java.io.PrintWriter;

import static org.objectweb.asm.Opcodes.*;

public class Rewriter {

    private final ClassReader classReader;
    private final ClassWriter classWriter;
    private final ClassVisitor classVisitor;

    private static int allocation_site = 0;

    public Rewriter(String className, byte[] classfileBuffer) {
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
            MethodVisitor methodVisitor = super.visitMethod(access, name, descriptor, signature, exceptions);
            assert methodVisitor != null;
            return new MethodAdapter(methodVisitor);
        }

    }

    private static class MethodAdapter extends MethodVisitor {

        private int bcid = 0;

        public MethodAdapter(MethodVisitor methodVisitor) {
            super(Opcodes.ASM9, methodVisitor);
        }

        @Override
        public void visitInsn(int opcode) {
            BCCAnalysis.bytecodeTransformed();
            mv.visitInsn(NOP);
            push(opcode);
            mv.visitInsn(NOP);
            push(++bcid);
            mv.visitMethodInsn(INVOKESTATIC, "org/dacapo/analysis/BCCAnalysis", "bytecodeExecuted", "(II)V");
            mv.visitInsn(opcode);
        }

        void push(final int value) {
            if (value >= -1 && value <= 5) {
                mv.visitInsn(Opcodes.ICONST_0 + value);
            } else if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) {
                mv.visitIntInsn(Opcodes.BIPUSH, value);
            } else if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) {
                mv.visitIntInsn(Opcodes.SIPUSH, value);
            } else {
                mv.visitLdcInsn(value);
            }
        }
    }
}
