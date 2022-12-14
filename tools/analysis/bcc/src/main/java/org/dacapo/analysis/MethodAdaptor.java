package org.dacapo.analysis;

import org.objectweb.asm.*;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.*;

import java.util.ListIterator;

class MethodAdaptor extends MethodNode {
        
    public MethodAdaptor(int access, String name, String descriptor, String signature, String[] exceptions, MethodVisitor mv) {
        super(Opcodes.ASM5, access, name, descriptor, signature, exceptions);
        this.mv = mv;
    }

    @Override
    public void visitEnd() {
        // ListIterator<AbstractInsnNode> itr = instructions.iterator();
        // while (itr.hasNext()) {
        //     AbstractInsnNode node = itr.next();
        //     InsnList callout = new InsnList();

        //     /* ... */

        //     callout.add(new InsnNode(Opcodes.NOP));
        //     instructions.insert(node, callout);
        //     maxStack = Math.max(5, maxStack);
        // }
        // accept(mv);
    }
}