package org.example;


import org.objectweb.asm.*;
import org.objectweb.asm.util.Printer;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceMethodVisitor;

import java.io.FileNotFoundException;
import java.io.PrintWriter;

import static org.objectweb.asm.Opcodes.*;

public class AllocRewriter {

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
            MethodVisitor methodVisitor = super.visitMethod(access, name, descriptor, signature, exceptions);
            assert methodVisitor != null;
//          I think I never make the TraceMethodVisitor work  

//            if (name.equals("test")) {
//                Printer p = new Textifier(ASM9) {
//                    @Override
//                    public void visitMethodEnd() {
//                        PrintWriter output = null;
//                        try {
//                            final String fileName = className.replaceAll("/", "-") + name;
//                            output = new PrintWriter(String.format("/home/qtl/Programming/tmp/%s.txt", fileName));
//                        } catch (FileNotFoundException e) {
//                            throw new RuntimeException(e);
//                        }
//                        print(output);
//                        output.flush();
//                    }
//                };
//                return new MethodAdapter(new TraceMethodVisitor(methodVisitor, p));
//            }
            return new MethodAdapter(methodVisitor);
        }

    }

    private static class MethodAdapter extends MethodVisitor {
        private static final int INVALID_BYTECODE = 255;
        private int previous_bytecode = INVALID_BYTECODE;

        private static final String owner = "org/example/Hello";

        public MethodAdapter(MethodVisitor methodVisitor) {
            super(Opcodes.ASM9, methodVisitor);
        }


        @Override
        public void visitTypeInsn(int opcode, String type) {
            previous_bytecode = opcode;
            switch (opcode) {
                case NEW:
                case ANEWARRAY:
                    mv.visitTypeInsn(opcode, type);
                    push(++allocation_site);
                    mv.visitMethodInsn(INVOKESTATIC, owner, "hello", "(I)V", false);
                    previous_bytecode = INVALID_BYTECODE;
                    return;
//                    System.out.println("new/anewarray: " + ++allocation_site);
            }
            super.visitTypeInsn(opcode, type);
        }

        @Override
        public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
            mv.visitMultiANewArrayInsn(descriptor, numDimensions);
            mv.visitInsn(NOP);
            push(++allocation_site);
            mv.visitMethodInsn(INVOKESTATIC, owner, "hello", "(I)V", false);
            previous_bytecode = INVALID_BYTECODE;
        }

        @Override
        public void visitIntInsn(int opcode, int operand) {
            previous_bytecode = opcode;
            if (opcode == NEWARRAY) {
//                System.out.println("newarray: " + ++allocation_site);
                mv.visitIntInsn(opcode, operand);
                push(++allocation_site);
                mv.visitMethodInsn(INVOKESTATIC, owner, "hello", "(I)V", false);
                previous_bytecode = INVALID_BYTECODE;
                return;
            }
            super.visitIntInsn(opcode, operand);
        }

//        @Override
//        public void visitInsn(int opcode) {
//            if (opcode == DUP) {
//                switch (previous_bytecode) {
//                    case MULTIANEWARRAY:
//                    case ANEWARRAY:
//                    case NEWARRAY:
////                    case NEW:
//                        mv.visitInsn(opcode);
//                        mv.visitInsn(opcode);
//                        push(allocation_site);
//                        mv.visitMethodInsn(INVOKESTATIC, owner, "hello", "(I)V", false);
//                        previous_bytecode = INVALID_BYTECODE;
//                        return;
//                }
//            }
//            super.visitInsn(opcode);
//        }

        public void push(final int value) {
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
