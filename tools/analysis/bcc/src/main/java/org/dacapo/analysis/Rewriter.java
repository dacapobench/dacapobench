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
            instrumentInsn(opcode);
            mv.visitInsn(opcode);
        }

        @Override
        public void visitIntInsn(int opcode, int operand) {
            instrumentInsn(opcode);
            mv.visitIntInsn(opcode, operand);
        }

        @Override
        public void visitVarInsn(int opcode, int var) {
            instrumentInsn(opcode);
            mv.visitVarInsn(opcode, var);
        }

        @Override
        public void visitTypeInsn(int opcode, String type) {
            instrumentInsn(opcode);
            mv.visitTypeInsn(opcode, type);
        }

        @Override
        public void visitFieldInsn(int opcode, String owner, String name, String desc) {
            instrumentInsn(opcode);
            mv.visitFieldInsn(opcode, owner, name, desc);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
            instrumentInsn(opcode);
            mv.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
        }

        @Override
        public void visitInvokeDynamicInsn(String name, String desc, Handle bsm,
            Object... bsmArgs) {
            instrumentInsn(Opcodes.INVOKEDYNAMIC);
            mv.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
        }
        
        @Override
        public void visitJumpInsn(int opcode, Label label) {
            instrumentInsn(opcode);
            mv.visitJumpInsn(opcode, label);
        }

        @Override
        public void visitLdcInsn(Object cst) {
            instrumentInsn(Opcodes.LDC);
            mv.visitLdcInsn(cst);
        }

        @Override
        public void visitIincInsn(int var, int increment) {
            instrumentInsn(Opcodes.IINC);
            mv.visitIincInsn(var, increment);
        }

        @Override
        public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
            instrumentInsn(Opcodes.TABLESWITCH);
            mv.visitTableSwitchInsn(min, max, dflt, labels);
        }

        @Override
        public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
            instrumentInsn(Opcodes.LOOKUPSWITCH);
            mv.visitLookupSwitchInsn(dflt, keys, labels);
        }

        @Override
        public void visitMultiANewArrayInsn(String desc, int dims) {
            instrumentInsn(Opcodes.MULTIANEWARRAY);
            mv.visitMultiANewArrayInsn(desc, dims);
        }

        void instrumentInsn(int opcode) {
            bcid++;
            int id = BCCAnalysis.bytecodeTransformed();

            mv.visitInsn(NOP);
            push(opcode);
            mv.visitInsn(NOP);            
            push(id);
            mv.visitMethodInsn(INVOKESTATIC, "org/dacapo/analysis/BCCAnalysis", "bytecodeExecuted", "(II)V", false);
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
