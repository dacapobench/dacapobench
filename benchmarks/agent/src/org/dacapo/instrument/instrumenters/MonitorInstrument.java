package org.dacapo.instrument.instrumenters;

import java.util.Properties;
import java.util.TreeMap;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

public class MonitorInstrument extends Instrumenter {

	public static final Class[]   DEPENDENCIES = new Class[] { SystemInstrument.class };

	public static final String    MONITOR                    = "monitor";

	private static final String   LOG_ENTER_METHOD           = "logMonitorEnter";
	private static final String   LOG_EXIT_METHOD            = "logMonitorExit";
	private static final String   LOG_NOTIFY_METHOD          = "logMonitorNotify";
	private static final String   LOG_INTERNAL_ENTER_METHOD  = INTERNAL_LOG_PREFIX + LOG_ENTER_METHOD;
	private static final String   LOG_INTERNAL_EXIT_METHOD   = INTERNAL_LOG_PREFIX + LOG_EXIT_METHOD;
	private static final String   LOG_INTERNAL_NOTIFY_METHOD = INTERNAL_LOG_PREFIX + LOG_NOTIFY_METHOD;
	private static final String   LOG_CLASS_SIGNATURE        = Type.getMethodDescriptor(Type.VOID_TYPE, new Type[0]);
	private static final String   LOG_OBJECT_SIGNATURE       = Type.getMethodDescriptor(Type.VOID_TYPE, new Type[] { JAVA_LANG_OBJECT_TYPE });
	
	private static final String   NOTIFY_METHOD              = "notify";
	private static final String   NOTIFY_ALL_METHOD          = "notifyAll";
	private static final String   NOTIFY_SIGNATURE           = LOG_CLASS_SIGNATURE;
	
	private String                className                  = null;
	private int                   access                     = 0;

	private boolean				  has_monitor_operation      = false;
	private boolean				  has_monitor_notify         = true;
	private boolean               classDone                  = false;

	public static ClassVisitor make(ClassVisitor cv, TreeMap<String,Integer> methodToLargestLocal,
			Properties options, Properties state) {
		if (options.containsKey(MONITOR))
			cv = new MonitorInstrument(cv, methodToLargestLocal, options, state);
		return cv;
	}
	
	private MonitorInstrument(ClassVisitor cv, TreeMap<String,Integer> methodToLargestLocal,
			Properties options, Properties state) {
		super(cv, methodToLargestLocal, options, state);
	}
	
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		this.className  = name;
		this.access     = access;
		super.visit(version, access, name, signature, superName, interfaces);
	}
	
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		if (instrument() && !isGenerated(name) && instrument(access)) {
			return new MonitorInstrumentMethod(access, name, desc, signature, exceptions, super.visitMethod(access,name,desc,signature,exceptions));
		} else {
			return super.visitMethod(access,name,desc,signature,exceptions);
		}
	}

	@SuppressWarnings("unchecked")
	public void visitEnd() {
		if (!classDone && (access & Opcodes.ACC_INTERFACE) == 0) {
			classDone = true;
			if (has_monitor_operation)
				try {
					Type thisClassType = Type.getObjectType(className);
					GeneratorAdapter mg;
					Label start;
					Label end;
	
					// generate Log monitorEnter function
					mg = new GeneratorAdapter(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, new Method(LOG_INTERNAL_ENTER_METHOD, LOG_OBJECT_SIGNATURE), LOG_OBJECT_SIGNATURE, new Type[] {}, this);
					
					mg.loadArg(0);
					start = mg.mark();
					mg.invokeStatic(LOG_INTERNAL_TYPE, Method.getMethod(LOG_INTERNAL_CLASS.getMethod(LOG_ENTER_METHOD, Object.class)));
					end   = mg.mark();
					mg.returnValue();
					
					mg.catchException(start, end, JAVA_LANG_THROWABLE_TYPE);
					mg.pop();
					mg.returnValue();
					
					mg.endMethod();
					
					// generate Log monitorExit function
					mg = new GeneratorAdapter(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, new Method(LOG_INTERNAL_EXIT_METHOD, LOG_OBJECT_SIGNATURE), LOG_OBJECT_SIGNATURE, new Type[] {}, this);
					
					mg.loadArg(0);
					start = mg.mark();
					mg.invokeStatic(LOG_INTERNAL_TYPE, Method.getMethod(LOG_INTERNAL_CLASS.getMethod(LOG_EXIT_METHOD, Object.class)));
					end   = mg.mark();
					mg.returnValue();
					
					mg.catchException(start, end, LOG_INTERNAL_TYPE);
					mg.pop();
					mg.returnValue();
					
					mg.endMethod();
					
					// generate Log monitorEnter function
					mg = new GeneratorAdapter(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, new Method(LOG_INTERNAL_ENTER_METHOD, LOG_CLASS_SIGNATURE), LOG_CLASS_SIGNATURE, new Type[] {}, this);
					
					mg.push(thisClassType);
					mg.invokeStatic(thisClassType, new Method(LOG_INTERNAL_ENTER_METHOD, LOG_OBJECT_SIGNATURE));
					mg.returnValue();
					
					mg.endMethod();
					
					// generate Log monitorExit function
					mg = new GeneratorAdapter(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, new Method(LOG_INTERNAL_EXIT_METHOD, LOG_CLASS_SIGNATURE), LOG_CLASS_SIGNATURE, new Type[] {}, this);
					
					mg.push(thisClassType);
					mg.invokeStatic(thisClassType, new Method(LOG_INTERNAL_EXIT_METHOD, LOG_OBJECT_SIGNATURE));
					mg.returnValue();
					
					mg.endMethod();
					
					if (has_monitor_notify) {
						// generate Log notify function we always have an object of the invocation of notify and notifyAll
						mg = new GeneratorAdapter(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, new Method(LOG_INTERNAL_NOTIFY_METHOD, LOG_OBJECT_SIGNATURE), LOG_OBJECT_SIGNATURE, new Type[] {}, this);
						
						mg.loadArg(0);
						start = mg.mark();
						mg.invokeStatic(LOG_INTERNAL_TYPE, Method.getMethod(LOG_INTERNAL_CLASS.getMethod(LOG_NOTIFY_METHOD, Object.class)));
						end   = mg.mark();
						mg.returnValue();
						
						mg.catchException(start, end, JAVA_LANG_THROWABLE_TYPE);
						mg.pop();
						mg.returnValue();
						
						mg.endMethod();
					}
				} catch (NoSuchMethodException nsme) {
					System.err.println("Unable to find Agent.logMonitorEnter, Agent.logMonitorExit or Agent.logMonitorNotify method");
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
		private int      access;
		private Label    methodStartLabel;
		private String[] exceptions;
		
		private boolean  done = false;
		
		MonitorInstrumentMethod(int access, String name, String desc, String signature, String[] exceptions, MethodVisitor mv) {
			super(mv, access, name, desc);
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
				JAVA_LANG_OBJECT.equals(owner) &&
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
				if (exceptions!=null) {
					for(String ex: exceptions) {
						super.catchException(methodStartLabel,methodEndLabel,Type.getObjectType(ex));
						super.visitMethodInsn(Opcodes.INVOKESTATIC, className, LOG_INTERNAL_EXIT_METHOD, LOG_CLASS_SIGNATURE);
						super.visitInsn(Opcodes.ATHROW);
					}
				}
				super.catchException(methodStartLabel,methodEndLabel,Type.getType(RuntimeException.class));
				super.visitMethodInsn(Opcodes.INVOKESTATIC, className, LOG_INTERNAL_EXIT_METHOD, LOG_CLASS_SIGNATURE);
				super.visitInsn(Opcodes.ATHROW);
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
