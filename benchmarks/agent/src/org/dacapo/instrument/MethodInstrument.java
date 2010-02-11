package org.dacapo.instrument;

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

public class MethodInstrument extends ClassAdapter {

	private static final int      CLINIT_ACCESS        = Opcodes.ACC_STATIC;
	private static final String   CLINIT_NAME          = "<clinit>";
	private static final String   CLINIT_DESCRIPTION   = null;
	private static final String   CLINIT_SIGNATURE     = "()V";
	private static final String[] CLINIT_EXCEPTIONS    = { };

	private static final String   JAVA_PACKAGE         = "java/";
	
	private static final String   INSTRUMENT_PACKAGE   = "org/dacapo/instrument/";
	
	private static final String   LOG_INTERNAL_NAME    = "org/dacapo/instrument/Log";
	private static final String   LOG_METHOD_NAME      = "reportMethod";
	private static final String   LOG_METHOD_SIGNATURE = "(Ljava/lang/String;Ljava/lang/String;)Z"; 
	
	private static final Integer  ZERO                 = new Integer(0);
	
	private boolean               foundClinit = false;
	private int                   access      = 0;
	private ClassVisitor          cv          = null;
	private String                className   = null;
	
	public MethodInstrument(ClassVisitor cv, String className) {
		super(cv);
		this.cv = cv;
		this.className = className;
	}
	
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		this.access = access;
		super.visit(version, access, name, signature, superName, interfaces);
	}
	
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		if (instrument() && instrument(name,access)) {
			return new MethodInstrumentMethod(access, name, desc, signature, exceptions, super.visitMethod(access,name,desc,signature,exceptions));
		} else {
			return super.visitMethod(access,name,desc,signature,exceptions);
		}
	}
	
	public void visitEnd() {
		super.visitEnd();
	}
	
	private boolean instrument() {
		return !className.startsWith(INSTRUMENT_PACKAGE) && (access & Opcodes.ACC_INTERFACE) == 0;
	}

	boolean instrument(String name, int access) {
		return (access & Opcodes.ACC_ABSTRACT) == 0 && (access & Opcodes.ACC_BRIDGE) == 0 && (access & Opcodes.ACC_NATIVE) == 0 && !CLINIT_NAME.equals(name);
	}
	
	private class MethodInstrumentMethod extends AdviceAdapter {
		private String name;
		private String flagName;
		private int    access;
		
		MethodInstrumentMethod(int access, String name, String desc, String signature, String[] exceptions, MethodVisitor mv) {
			super(mv, access, name, desc);
			this.name = name;
			this.flagName = "_$$"+name;
			this.access = access;
		}
		
		protected void onMethodEnter() {
			Label target = super.newLabel();

			visitField(Opcodes.ACC_STATIC + Opcodes.ACC_PRIVATE,flagName,Type.BOOLEAN_TYPE.getDescriptor(),null,ZERO);
			
			super.visitFieldInsn(Opcodes.GETSTATIC,className,flagName,Type.BOOLEAN_TYPE.getDescriptor());
			super.visitJumpInsn(Opcodes.IFNE,target);
			Label start = super.mark();
			super.visitLdcInsn(className);
			super.visitLdcInsn(name);
			super.visitMethodInsn(Opcodes.INVOKESTATIC, LOG_INTERNAL_NAME, LOG_METHOD_NAME, LOG_METHOD_SIGNATURE);
			super.visitFieldInsn(Opcodes.PUTSTATIC,className,flagName,Type.BOOLEAN_TYPE.getDescriptor());
			Label end = super.mark();

			super.visitJumpInsn(Opcodes.GOTO,target);
			super.catchException(start,end,Type.getType(NoClassDefFoundError.class));
			super.pop(); // for some unknown reason using super.visitInsn(Opcodes.POP); here causes an indexing error in ASM
			super.mark(target);
			super.visitInsn(Opcodes.NOP);
		}
	}
	
}
