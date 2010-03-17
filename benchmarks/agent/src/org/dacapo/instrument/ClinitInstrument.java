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

public class ClinitInstrument  extends ClassAdapter {

	private static final int      CLINIT_ACCESS        = Opcodes.ACC_STATIC;
	private static final String   CLINIT_NAME          = "<clinit>";
	private static final String   CLINIT_DESCRIPTION   = null;
	private static final String   CLINIT_SIGNATURE     = "()V";
	private static final String[] CLINIT_EXCEPTIONS    = { };

	private static final String   JAVA_PACKAGE         = "java/";
	
	private static final String   INSTRUMENT_PACKAGE   = "org/dacapo/instrument/";
	
	private static final String   LOG_INTERNAL_NAME    = "org/dacapo/instrument/Log";
	private static final String   LOG_METHOD_NAME      = "reportClass";
	private static final String   LOG_METHOD_SIGNATURE = "(Ljava/lang/String;)V"; 
	
	private boolean             foundClinit = false;
	private int                 access      = 0;
	private ClassVisitor        cv          = null;
	private String              className   = null;
	
	public ClinitInstrument(ClassVisitor cv, String className) {
		super(cv);
		this.cv = cv;
		this.className = className;
	}
	
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		this.access = access;
		this.foundClinit = false;
		super.visit(version, access, name, signature, superName, interfaces);
	}
	
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		if (!foundClinit && standardMethod(access,true) && CLINIT_NAME.equals(name) && instrument()) {
			foundClinit = true;
			
			return new ClinitInstrumentMethod(access, name, desc, signature, exceptions, super.visitMethod(access,name,desc,signature,exceptions));
		} else {
			return super.visitMethod(access,name,desc,signature,exceptions);
		}
	}
	
	public void visitEnd() {
		if (!foundClinit && instrument()) {
			// didn't find <clinit> so lets make one
			try {
				Class k = Log.class;
				GeneratorAdapter mg = new GeneratorAdapter(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, new Method(CLINIT_NAME, CLINIT_SIGNATURE), CLINIT_SIGNATURE, new Type[] {}, this);
				
				Label start = mg.mark();
				mg.push(className);
				mg.invokeStatic(Type.getType(k), Method.getMethod(k.getMethod(LOG_METHOD_NAME, String.class)));
				Label end   = mg.mark();
				mg.returnValue();
				
				mg.catchException(start, end, Type.getType(Throwable.class));
				mg.returnValue();
				
				mg.endMethod();
			} catch (NoSuchMethodException nsme) {
				System.out.println("Unable to find Log.reportClass method");
			}
		}
		
		super.visitEnd();
	}
	
	private boolean instrument() {
		return (access & Opcodes.ACC_INTERFACE) == 0 && !className.startsWith(INSTRUMENT_PACKAGE);
	}

	private static boolean standardMethod(int access, boolean isStatic) {
		return (access & Opcodes.ACC_NATIVE) == 0 && (access & Opcodes.ACC_ABSTRACT) == 0 && (access & Opcodes.ACC_STATIC)==(isStatic?Opcodes.ACC_STATIC:0); 
	}
	
	private class ClinitInstrumentMethod extends AdviceAdapter {
		ClinitInstrumentMethod(int access, String name, String desc, String signature, String[] exceptions, MethodVisitor mv) {
			super(mv, access, name, desc);
		}
		
		protected void onMethodEnter() {
			Label target = super.newLabel();
			// invoke the logger, note that we may not know about the Log class yet so we must catch and ignore the
			// exception here.
			super.visitLdcInsn(className);
			Label start = super.mark();
			super.visitMethodInsn(Opcodes.INVOKESTATIC, LOG_INTERNAL_NAME, LOG_METHOD_NAME, LOG_METHOD_SIGNATURE);
			Label end = super.mark();
			super.visitJumpInsn(Opcodes.GOTO,target);
			// catch the exception, discard the exception
			super.catchException(start,end,Type.getType(Throwable.class));
			super.pop();
			super.mark(target);
			// remainder of the <clinit>
		}
	}
}
