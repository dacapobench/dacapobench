package org.dacapo.instrument;

import java.util.Comparator;
import java.util.TreeMap;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

import org.objectweb.asm.commons.LocalVariablesSorter;

public class MonitorInstrument extends ClassAdapter {

	private static final String   LOG_INTERNAL_NAME          = "org/dacapo/instrument/Log";
	private static final String   LOG_ENTER_METHOD           = "reportMonitorEnter";
	private static final String   LOG_EXIT_METHOD            = "reportMonitorExit";
	private static final String   LOG_NOTIFY_METHOD          = "reportMonitorNotify";
	private static final String   LOG_INTERNAL_ENTER_METHOD  = "$$monitorEnter";
	private static final String   LOG_INTERNAL_EXIT_METHOD   = "$$monitorExit";
	private static final String   LOG_INTERNAL_NOTIFY_METHOD = "$$monitorNotify";
	private static final String   LOG_CLASS_SIGNATURE        = "()V";
	private static final String   LOG_OBJECT_SIGNATURE       = "(Ljava/lang/Object;)V";
	
	private static final String   OBJECT_TYPE_NAME           = "java/lang/Object";
	private static final String   NOTIFY_METHOD              = "notify";
	private static final String   NOTIFY_ALL_METHOD          = "notifyAll";
	private static final String   NOTIFY_SIGNATURE           = "()V";
	
	private ClassVisitor          cv                         = null;

	private String                className                  = null;
	private String                name                       = null;
	private int                   access                     = 0;

	private boolean				  has_monitor_operation      = false;
	private boolean				  has_monitor_notify         = true;
	private boolean               classDone                  = false;
	
	public MonitorInstrument(ClassVisitor cv) {
		super(cv);
		this.cv = cv;
	}
	
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		this.className  = name;
		this.access     = access;
		if ((version&0xffff)<49)
			version = 49;
		super.visit(version, access, name, signature, superName, interfaces);
	}
	
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		if (instrument() && instrument(access)) {
			return new MonitorInstrumentMethod(access, name, desc, signature, exceptions, super.visitMethod(access,name,desc,signature,exceptions));
		} else {
			return super.visitMethod(access,name,desc,signature,exceptions);
		}
	}

	public void visitEnd() {
		if (!classDone && (access & Opcodes.ACC_INTERFACE) == 0) {
			classDone = true;
			if (has_monitor_operation)
				try {
					Class k = Log.class;
					
					GeneratorAdapter mg;
					Label start;
					Label end;
	
					// generate Log monitorEnter function
					mg = new GeneratorAdapter(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, new Method(LOG_INTERNAL_ENTER_METHOD, LOG_OBJECT_SIGNATURE), LOG_OBJECT_SIGNATURE, new Type[] {}, this);
					
					mg.loadArg(0);
					start = mg.mark();
					mg.invokeStatic(Type.getType(k), Method.getMethod(k.getMethod(LOG_ENTER_METHOD, Object.class)));
					end   = mg.mark();
					mg.returnValue();
					
					mg.catchException(start, end, Type.getType(Throwable.class));
					mg.pop();
					mg.returnValue();
					
					mg.endMethod();
					
					// generate Log monitorExit function
					mg = new GeneratorAdapter(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, new Method(LOG_INTERNAL_EXIT_METHOD, LOG_OBJECT_SIGNATURE), LOG_OBJECT_SIGNATURE, new Type[] {}, this);
					
					mg.loadArg(0);
					start = mg.mark();
					mg.invokeStatic(Type.getType(k), Method.getMethod(k.getMethod(LOG_EXIT_METHOD, Object.class)));
					end   = mg.mark();
					mg.returnValue();
					
					mg.catchException(start, end, Type.getType(Throwable.class));
					mg.pop();
					mg.returnValue();
					
					mg.endMethod();
					
					// generate Log monitorEnter function
					mg = new GeneratorAdapter(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, new Method(LOG_INTERNAL_ENTER_METHOD, LOG_CLASS_SIGNATURE), LOG_CLASS_SIGNATURE, new Type[] {}, this);
					
					mg.push(Type.getObjectType(className));
					mg.invokeStatic(Type.getObjectType(className), new Method(LOG_INTERNAL_ENTER_METHOD, LOG_OBJECT_SIGNATURE));
					mg.returnValue();
					
					mg.endMethod();
					
					// generate Log monitorExit function
					mg = new GeneratorAdapter(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, new Method(LOG_INTERNAL_EXIT_METHOD, LOG_CLASS_SIGNATURE), LOG_CLASS_SIGNATURE, new Type[] {}, this);
					
					mg.push(Type.getObjectType(className));
					mg.invokeStatic(Type.getObjectType(className), new Method(LOG_INTERNAL_EXIT_METHOD, LOG_OBJECT_SIGNATURE));
					mg.returnValue();
					
					mg.endMethod();
					
					if (has_monitor_notify) {
						// generate Log notify function we always have an object of the invocation of notify and notifyAll
						mg = new GeneratorAdapter(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, new Method(LOG_INTERNAL_NOTIFY_METHOD, LOG_OBJECT_SIGNATURE), LOG_OBJECT_SIGNATURE, new Type[] {}, this);
						
						mg.loadArg(0);
						start = mg.mark();
						mg.invokeStatic(Type.getType(k), Method.getMethod(k.getMethod(LOG_NOTIFY_METHOD, Object.class)));
						end   = mg.mark();
						mg.returnValue();
						
						mg.catchException(start, end, Type.getType(Throwable.class));
						mg.pop();
						mg.returnValue();
						
						mg.endMethod();
					}
				} catch (NoSuchMethodException nsme) {
					System.err.println("Unable to find Log.reportMonitorEnter or Log.reportMonitorExit method");
					System.err.println("M:"+nsme);
					nsme.printStackTrace();
				}
		}
		
		super.visitEnd();
	}

	private boolean instrument() {
		return (access & Opcodes.ACC_INTERFACE) == 0;
	}
	
	private boolean instrument(int access) {
		return 
			(access & Opcodes.ACC_ABSTRACT) == 0 && 
			(access & Opcodes.ACC_BRIDGE) == 0 && 
			(access & Opcodes.ACC_NATIVE) == 0;
	}

	private class MonitorInstrumentMethod extends AdviceAdapter {
		private String   name;
		private int      access;
		private Label    methodStartLabel;
		private String[] exceptions;
		
		private boolean  done = false;
		
		MonitorInstrumentMethod(int access, String name, String desc, String signature, String[] exceptions, MethodVisitor mv) {
			super(mv, access, name, desc);
			this.name       = name;
			this.access     = access;
			this.exceptions = exceptions;
		}
		
		public void visitInsn(int opcode) {
			if (opcode == Opcodes.MONITORENTER) {
				has_monitor_operation = true;
				super.dup();
				super.visitInsn(opcode);
				super.visitMethodInsn(Opcodes.INVOKESTATIC, className, LOG_INTERNAL_ENTER_METHOD, LOG_OBJECT_SIGNATURE);
			} else if (opcode == Opcodes.MONITOREXIT) {
				has_monitor_operation = true;
				super.dup();
				super.visitMethodInsn(Opcodes.INVOKESTATIC, className, LOG_INTERNAL_EXIT_METHOD, LOG_OBJECT_SIGNATURE);
				super.visitInsn(opcode);
			} else {
				super.visitInsn(opcode);
			}
		}
		
		public void visitMethodInsn(int opcode, String owner, String name, String desc) {
			if (opcode == Opcodes.INVOKEVIRTUAL &&
				OBJECT_TYPE_NAME.equals(owner) &&
				NOTIFY_SIGNATURE.equals(desc) && 
				(NOTIFY_METHOD.equals(name) || NOTIFY_ALL_METHOD.equals(name))) {
				super.dup();
				super.visitMethodInsn(Opcodes.INVOKESTATIC, className, LOG_INTERNAL_NOTIFY_METHOD, LOG_OBJECT_SIGNATURE);
			}
			super.visitMethodInsn(opcode, owner, name, desc);
		}
		
		public void visitEnd() {
			if (isSynchronized()) {
				has_monitor_operation = true;
				done = true;
				Label methodEndLabel = super.mark();
				super.catchException(methodStartLabel,methodEndLabel,Type.getType(RuntimeException.class));
				super.visitMethodInsn(Opcodes.INVOKESTATIC, className, LOG_INTERNAL_EXIT_METHOD, LOG_CLASS_SIGNATURE);
				super.visitInsn(Opcodes.ATHROW);
				if (exceptions!=null) {
					for(String ex: exceptions) {
						super.catchException(methodStartLabel,methodEndLabel,Type.getObjectType(ex));
						super.visitMethodInsn(Opcodes.INVOKESTATIC, className, LOG_INTERNAL_EXIT_METHOD, LOG_CLASS_SIGNATURE);
						super.visitInsn(Opcodes.ATHROW);
					}
				}
			}
			super.visitEnd();
		}
		
		protected void onMethodEnter() {
			if (done || ! isSynchronized()) return;
			
			has_monitor_operation = true;
			super.visitInsn(Opcodes.ACONST_NULL);
			super.pop();
			methodStartLabel = super.mark();

			super.visitMethodInsn(Opcodes.INVOKESTATIC, className, LOG_INTERNAL_ENTER_METHOD, LOG_CLASS_SIGNATURE);
		}
		
		protected void onMethodExit(int opcode) {
			if (done || opcode == Opcodes.ATHROW || !isSynchronized()) return;
			
			has_monitor_operation = true;
			super.visitMethodInsn(Opcodes.INVOKESTATIC, className, LOG_INTERNAL_EXIT_METHOD, LOG_CLASS_SIGNATURE);
		}
		
		private boolean isSynchronized() {
			return (access & Opcodes.ACC_SYNCHRONIZED) != 0;
		}
	}

}
