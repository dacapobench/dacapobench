package org.dacapo.instrument.instrumenters;

import java.util.LinkedList;
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

public class ClinitInstrument  extends Instrumenter {

	public static final Class[]   DEPENDENCIES = new Class[] { MonitorInstrument.class };

	public static final String    CLASSES_INITIALIZATION = "clinit";

	private static final String   CLINIT_NAME          = "<clinit>";
	private static final String   CLINIT_SIGNATURE     = Type.getMethodDescriptor(Type.VOID_TYPE, new Type[0]);

	private static final String   LOG_METHOD_NAME      = "reportClass";
	private static final String   LOG_METHOD_SIGNATURE = Type.getMethodDescriptor(Type.VOID_TYPE, new Type[] { JAVA_LANG_STRING_TYPE });
	
	private boolean             foundClinit = false;
	private int                 access      = 0;
	private String              className   = null;
	
	private LinkedList<String>  excludePackages = new LinkedList<String>();

	public static ClassVisitor make(ClassVisitor cv, TreeMap<String,Integer> methodToLargestLocal, 
			Properties options, Properties state) {
		if (options.containsKey(CLASSES_INITIALIZATION))
			cv = new ClinitInstrument(cv, methodToLargestLocal, options, state, options.getProperty(CLASSES_INITIALIZATION)); 
		return cv;
	}
	
	protected ClinitInstrument(ClassVisitor cv, TreeMap<String,Integer> methodToLargestLocal, 
			Properties options, Properties state, String excludeList) {
		super(cv, methodToLargestLocal, options, state);
		
		excludePackages.add(INSTRUMENT_PACKAGE);
		if (excludeList!=null) {
			String[] packageList = excludeList.split(",");
			for(String p: packageList) {
				excludePackages.add(p);
			}
		}
	}
	
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		this.access = access;
		this.foundClinit = false;
		this.className = name;
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
	
	@SuppressWarnings("unchecked")
	public void visitEnd() {
		if (!foundClinit && instrument()) {
			// didn't find <clinit> so lets make one
			try {
				GeneratorAdapter mg = new GeneratorAdapter(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, new Method(CLINIT_NAME, CLINIT_SIGNATURE), CLINIT_SIGNATURE, new Type[] {}, this);
				
				Label start = mg.mark();
				mg.push(className);
				mg.invokeStatic(LOG_INTERNAL_TYPE, Method.getMethod(LOG_INTERNAL_CLASS.getMethod(LOG_METHOD_NAME, String.class)));
				Label end   = mg.mark();
				mg.returnValue();
				
				mg.catchException(start, end, JAVA_LANG_THROWABLE_TYPE);
				mg.returnValue();
				
				mg.endMethod();
			} catch (NoSuchMethodException nsme) {
				System.out.println("Unable to find Agent.reportClass method");
			}
		}
		
		super.visitEnd();
	}
	
	private boolean instrument() {
		if ((access & Opcodes.ACC_INTERFACE) != 0) return false;
		
		for(String p: excludePackages) {
			if (className.startsWith(p)) return false;
		}
		
		return true;
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
			super.visitMethodInsn(Opcodes.INVOKESTATIC, LOG_INTERNAL, LOG_METHOD_NAME, LOG_METHOD_SIGNATURE);
			Label end = super.mark();
			super.visitJumpInsn(Opcodes.GOTO,target);
			// catch the exception, discard the exception
			super.catchException(start,end,JAVA_LANG_THROWABLE_TYPE);
			super.pop();
			super.mark(target);
			// remainder of the <clinit>
		}
	}
}
