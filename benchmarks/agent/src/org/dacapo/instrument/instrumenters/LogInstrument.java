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

public class LogInstrument extends Instrumenter {
	
	public static final Class[]   DEPENDENCIES = new Class[] { MethodInstrument.class };

	public static final String    LOG_START            = "log_start";
	public static final String    LOG_STOP             = "log_stop";
	
	private static final String   LOG_METHOD_START     = "start";
	private static final String   LOG_METHOD_STOP      = "stop";
	private static final String   LOG_METHOD_SIGNATURE = Type.getMethodDescriptor(Type.VOID_TYPE, new Type[0]); 
	
	private static final String   LOG_INTERNAL_START_METHOD = INTERNAL_LOG_PREFIX + LOG_METHOD_START;
	private static final String   LOG_INTERNAL_STOP_METHOD  = INTERNAL_LOG_PREFIX + LOG_METHOD_STOP;
	private static final String   LOG_INTERNAL_SIGNATURE    = LOG_METHOD_SIGNATURE;
	
	private String                logOnClass           = null;
	private String                logOnMethod          = null;
	private String                logOnSignature       = null;
	private String                logOffClass          = null;
	private String                logOffMethod         = null;
	private String                logOffSignature      = null;

	private String                className            = null;
	private int                   access               = 0;

	
	public static void main(String[] args) {
		for(String a: args) {
			System.out.println(a);
			System.out.println("  "+getClassFrom(a));
			System.out.println("  "+getMethodFrom(a));
			System.out.println("  "+getSignatureFrom(a));
		}
	}

	public static ClassVisitor make(ClassVisitor cv, TreeMap<String,Integer> methodToLargestLocal, 
			Properties options, Properties state) {
		if (options.containsKey(LOG_START)) {
			String startMethod = options.getProperty(LOG_START);
			String stopMethod  = options.getProperty(LOG_STOP);
			
			if (stopMethod != null) {
				cv = new LogInstrument(cv, methodToLargestLocal, options, state, startMethod, stopMethod);
			} else {
				cv = new LogInstrument(cv, methodToLargestLocal, options, state, startMethod);
			}
		}
		return cv;
	}
		
	protected LogInstrument(ClassVisitor cv, TreeMap<String,Integer> methodToLargestLocal, 
			Properties options, Properties state, String logOn) {
		this(cv,methodToLargestLocal,options,state,logOn,logOn);
	}
	
	protected LogInstrument(ClassVisitor cv, TreeMap<String,Integer> methodToLargestLocal, 
			Properties options, Properties state, 
			String logOn, String logOff) {
		super(cv,methodToLargestLocal, options, state);
		this.logOnClass      = getClassFrom(logOn);
		this.logOnMethod     = getMethodFrom(logOn);
		this.logOnSignature  = getSignatureFrom(logOn);
		this.logOffClass     = getClassFrom(logOff);
		this.logOffMethod    = getMethodFrom(logOff);
		this.logOffSignature = getSignatureFrom(logOff);
	}
	
	public String toString() {
		return "Start: "+logOnClass+"."+logOnMethod+logOnSignature+" Stop: "+logOffClass+"."+logOffMethod+logOffSignature;
	}
	
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		this.className  = name.replace('.', '/');
		this.access     = access;

		super.visit(version, access, name, signature, superName, interfaces);
	}
	
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		if (instrument() && !isGenerated(name) && instrument(name,desc,access)) {
			return new LogInstrumentMethod(access, name, desc, signature, exceptions, isEntryPoint(name, desc), isExitPoint(name, desc), super.visitMethod(access,name,desc,signature,exceptions));
		} else {
			return super.visitMethod(access,name,desc,signature,exceptions);
		}
	}

	@SuppressWarnings("unchecked")
	public void visitEnd() {
		if (instrument()) {
			try {
				GeneratorAdapter mg;
				Label start;
				Label end;
				
				// generate Log start function
				mg = new GeneratorAdapter(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, new Method(LOG_INTERNAL_START_METHOD, LOG_INTERNAL_SIGNATURE), LOG_INTERNAL_SIGNATURE, new Type[] {}, this);
				
				start = mg.mark();
				mg.invokeStatic(LOG_INTERNAL_TYPE, Method.getMethod(LOG_INTERNAL_CLASS.getMethod(LOG_METHOD_START)));
				end   = mg.mark();
				mg.returnValue();
				
				mg.catchException(start, end, JAVA_LANG_THROWABLE_TYPE);
				mg.returnValue();
				
				mg.endMethod();
				
				// generate Log stop function
				mg = new GeneratorAdapter(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, new Method(LOG_INTERNAL_STOP_METHOD, LOG_INTERNAL_SIGNATURE), LOG_INTERNAL_SIGNATURE, new Type[] {}, this);
				
				start = mg.mark();
				mg.invokeStatic(LOG_INTERNAL_TYPE, Method.getMethod(LOG_INTERNAL_CLASS.getMethod(LOG_METHOD_STOP)));
				end   = mg.mark();
				mg.returnValue();
				
				mg.catchException(start, end, JAVA_LANG_THROWABLE_TYPE);
				mg.returnValue();
				
				mg.endMethod();
			} catch (NoSuchMethodException nsme) {
				System.err.println("Unable to find Agent.stop or Agent.start method");
			}
		}
		
		super.visitEnd();
	}
	
	private boolean instrument(String name, String signature, int access) {
		return 
			(access & Opcodes.ACC_ABSTRACT) == 0 && 
			(access & Opcodes.ACC_BRIDGE) == 0 && 
			(access & Opcodes.ACC_NATIVE) == 0 &&
			(isEntryPoint(name, signature) || isExitPoint(name, signature));
	}
	
	private boolean isEntryPoint(String name, String signature) {
		return name.equals(logOnMethod) && signature!=null && signature.equals(logOnSignature);
	}
	
	private boolean isExitPoint(String name, String signature) {
		return name.equals(logOffMethod) && signature!=null && signature.equals(logOffSignature);
	}
	
	private boolean instrument() {
		return (access & Opcodes.ACC_INTERFACE) == 0 && (className.equals(logOnClass) || className.equals(logOffClass));
	}
	
	private static String getClassFrom(String fullName) {
		if (fullName == null)
			return null;
		
		// e.g.   org/dacapo/instrument/LogInstrument.getSignatureFrom(Ljava/lang/String;)Ljava/lang/String;
		//        ^                                 ^
		return fullName.substring(0,fullName.indexOf('.'));
	}
	
	private static String getMethodFrom(String fullName) {
		if (fullName == null)
			return null;
		
		// e.g.   org/dacapo/instrument/LogInstrument.getSignatureFrom(Ljava/lang/String;)Ljava/lang/String;
		//                                            ^              ^
		return fullName.substring(fullName.indexOf('.')+1,fullName.indexOf('('));
	}
	
	private static String getSignatureFrom(String fullName) {
		if (fullName == null)
			return null;

		// e.g.   org/dacapo/instrument/LogInstrument.getSignatureFrom(Ljava/lang/String;)Ljava/lang/String;
		//                                                            ^                                    ^
		return fullName.substring(fullName.indexOf('('));
	}

	private class LogInstrumentMethod extends AdviceAdapter {
		private boolean  entry;
		private boolean  exit;
		private Label    methodStartLabel;
		private String[] exceptions;
		
		private boolean  done = false;
		
		LogInstrumentMethod(int access, String name, String desc, String signature, String[] exceptions, boolean entry, boolean exit, MethodVisitor mv) {
			super(mv, access, name, desc);
			this.entry      = entry;
			this.exit       = exit;
			this.exceptions = exceptions;
		}
		
		public void visitEnd() {
			if (exit) {
				done = true;
				Label methodEndLabel = super.mark();
				super.catchException(methodStartLabel,methodEndLabel,Type.getType(RuntimeException.class));
				super.visitMethodInsn(Opcodes.INVOKESTATIC, className, LOG_INTERNAL_STOP_METHOD, LOG_METHOD_SIGNATURE);
				super.visitInsn(Opcodes.ATHROW);
				if (exceptions!=null) {
					for(String ex: exceptions) {
						super.catchException(methodStartLabel,methodEndLabel,Type.getObjectType(ex));
						super.visitMethodInsn(Opcodes.INVOKESTATIC, className, LOG_INTERNAL_STOP_METHOD, LOG_METHOD_SIGNATURE);
						super.visitInsn(Opcodes.ATHROW);
					}
				}
			}
			super.visitEnd();
		}
		
		protected void onMethodEnter() {
			if (exit)
				methodStartLabel = super.mark();

			if (!entry) return;
			
			super.visitMethodInsn(Opcodes.INVOKESTATIC, className, LOG_INTERNAL_START_METHOD, LOG_INTERNAL_SIGNATURE);
		}
		
		protected void onMethodExit(int opcode) {
			if (!exit || done) return;
			
			super.visitMethodInsn(Opcodes.INVOKESTATIC, className, LOG_INTERNAL_STOP_METHOD, LOG_INTERNAL_SIGNATURE);
		}
	}

}
