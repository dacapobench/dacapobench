package org.dacapo.instrument.instrumenters;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

import java.util.Properties;
import java.util.TreeMap;
import java.util.Set;

public class MethodInstrument extends Instrumenter {

	public static final Class[]   DEPENDENCIES = new Class[] { CallChainInstrument.class };

	public static final String    METHOD_INSTR           = "method_instr";

	private static final String   CLINIT_NAME            = "<clinit>";

	private static final String   LOG_METHOD_NAME        = "logMethod";
	private static final String   LOG_BRIDGE_SIGNATURE   = 
		Type.getMethodDescriptor(Type.VOID_TYPE, new Type[] { JAVA_LANG_STRING_TYPE, JAVA_LANG_STRING_TYPE });
	
	private static final String   LOG_INTERNAL_METHOD    = "$$" + LOG_METHOD_NAME;
	private static final String   LOG_INTERNAL_SIGNATURE = 
		Type.getMethodDescriptor(Type.BOOLEAN_TYPE, new Type[] { JAVA_LANG_STRING_TYPE, JAVA_LANG_STRING_TYPE });
	
	private static final Integer  ZERO                   = new Integer(0);
	
	private int                   access      = 0;
	private String                className   = null;
	private boolean               done        = false;
	
	private TreeMap<String,String>    methods = new TreeMap<String,String>();
	
	private Method                logBridgeMethod = null;

	public static ClassVisitor make(ClassVisitor cv, TreeMap<String,Integer> methodToLargestLocal, 
			Properties options, Properties state) {
		if (options.containsKey(METHOD_INSTR))
			cv = new MethodInstrument(cv, methodToLargestLocal, options, state, options.getProperty(PROP_CLASS_NAME));
		return cv;
	}
	
	protected MethodInstrument(ClassVisitor cv, TreeMap<String,Integer> methodToLargestLocal, 
			Properties options, Properties state, String className) {
		super(cv, methodToLargestLocal, options, state);
		this.className = className;
	}
	
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		this.access = access;
		super.visit(version, access, name, signature, superName, interfaces);
	}
	
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		if (!done && instrument() && instrument(name,access)) {
			return new MethodInstrumentMethod(access, name, desc, signature, exceptions, super.visitMethod(access,name,desc,signature,exceptions));
		} else {
			return super.visitMethod(access,name,desc,signature,exceptions);
		}
	}

	@SuppressWarnings("unchecked")
	public void visitEnd() {
		if (!done && (access & Opcodes.ACC_INTERFACE) == 0) {
			done = true;
			try {
				GeneratorAdapter mg;
				Label start;
				Label end;
				
				logBridgeMethod = new Method(LOG_INTERNAL_METHOD, LOG_INTERNAL_SIGNATURE);
				
				mg = new GeneratorAdapter(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, logBridgeMethod, LOG_INTERNAL_SIGNATURE, new Type[] {}, this);

				java.lang.reflect.Method m = LOG_INTERNAL_CLASS.getMethod(LOG_METHOD_NAME, String.class, String.class, String.class);

				start = mg.mark();
				mg.push(className);
				mg.loadArg(0);
				mg.loadArg(1);
				mg.invokeStatic(LOG_INTERNAL_TYPE, Method.getMethod(m));
				mg.returnValue();
				end   = mg.mark();
				mg.catchException(start, end, JAVA_LANG_THROWABLE_TYPE);
				mg.visitInsn(Opcodes.ICONST_0); // return false
				mg.returnValue();
				
				mg.endMethod();
				
				Set<String> keys = methods.keySet();
				
				for(String methName: keys) {
					makeMethod(methName, methods.get(methName));
				}
			} catch (NoSuchMethodException nsme) {
				System.err.println("Unable to find Agent.logMethod method");
				System.err.println("M:"+nsme);
				nsme.printStackTrace();
			}
		}
		
		super.visitEnd();
	}
	
	private void makeMethod(String methName, String flagName) {
		
		Type  classType = Type.getObjectType(className);
		
		GeneratorAdapter mg;

		visitField(Opcodes.ACC_STATIC | Opcodes.ACC_PRIVATE,flagName,Type.BOOLEAN_TYPE.getDescriptor(),null,ZERO);
		
		mg = new GeneratorAdapter(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, new Method(methName, LOG_BRIDGE_SIGNATURE), LOG_BRIDGE_SIGNATURE, new Type[] {}, this);

		Label end = mg.newLabel();
		
		// get static field
		mg.getStatic(classType,flagName,Type.BOOLEAN_TYPE);
		mg.ifZCmp(GeneratorAdapter.NE,end);
		mg.loadArg(0);
		mg.loadArg(1);
		mg.invokeStatic(classType,logBridgeMethod);
		mg.visitInsn(Opcodes.ICONST_0); // return false
		mg.putStatic(classType,flagName,Type.BOOLEAN_TYPE);
		mg.mark(end);
		mg.returnValue();
		
		mg.endMethod();
	}
	
	private boolean instrument() {
		return !className.startsWith(INSTRUMENT_PACKAGE) && (access & Opcodes.ACC_INTERFACE) == 0;
	}

	private boolean instrument(String name, int access) {
		return (access & Opcodes.ACC_ABSTRACT) == 0 && (access & Opcodes.ACC_BRIDGE) == 0 && (access & Opcodes.ACC_NATIVE) == 0 && !CLINIT_NAME.equals(name);
	}
	
	private class MethodInstrumentMethod extends AdviceAdapter {
		private String  name;
		private String  desc;
		private String  flagName;
		private String  methName;
		private boolean added = false;
		
		MethodInstrumentMethod(int access, String name, String desc, String signature, String[] exceptions, MethodVisitor mv) {
			super(mv, access, name, desc);
			this.name = name;
			this.desc = desc;
			this.flagName = INTERNAL_FIELD_PREFIX+(name+desc).replaceAll("[^\\p{Alnum}]","_");
			this.methName = INTERNAL_METHOD_PREFIX+(name+desc).replaceAll("[^\\p{Alnum}]","_");
		}
		
		protected void onMethodEnter() {
			if (done) return;

			if (!added) {
				added = true;
				methods.put(methName,flagName);
			}

			super.visitLdcInsn(name);
			super.visitLdcInsn(desc);
			super.visitMethodInsn(Opcodes.INVOKESTATIC, className, methName, LOG_BRIDGE_SIGNATURE);
		}
	}
	
}
