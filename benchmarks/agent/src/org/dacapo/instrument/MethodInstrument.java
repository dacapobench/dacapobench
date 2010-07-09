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

import java.util.TreeMap;
import java.util.Set;

public class MethodInstrument extends Instrument {

	private static final int      CLINIT_ACCESS          = Opcodes.ACC_STATIC;
	private static final String   CLINIT_NAME            = "<clinit>";
	private static final String   CLINIT_DESCRIPTION     = null;
	private static final String   CLINIT_SIGNATURE       = "()V";
	private static final String[] CLINIT_EXCEPTIONS      = { };

	private static final String   JAVA_PACKAGE           = "java/";
	
	private static final String   INSTRUMENT_PACKAGE     = "org/dacapo/instrument/";
	
	private static final String   LOG_INTERNAL_NAME      = "org/dacapo/instrument/Log";
	private static final String   LOG_METHOD_NAME        = "reportMethod";
	private static final String   LOG_METHOD_SIGNATURE   = "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Z"; 
	private static final String   LOG_BRIDGE_SIGNATURE   = "(Ljava/lang/String;Ljava/lang/String;)V";
	
	private static final String   LOG_INTERNAL_METHOD    = "$$reportMethod";
	private static final String   LOG_INTERNAL_SIGNATURE = "(Ljava/lang/String;Ljava/lang/String;)Z";
	
	private static final Integer  ZERO                   = new Integer(0);
	
	private int                   access      = 0;
	private ClassVisitor          cv          = null;
	private String                className   = null;
	private boolean               done        = false;
	
	private TreeMap<String,String>    methods = new TreeMap<String,String>();
	
	private Method                logBridgeMethod = null;
	
	public MethodInstrument(ClassVisitor cv, TreeMap<String,Integer> methodToLargestLocal, String className) {
		super(cv, methodToLargestLocal);
		this.cv = cv;
		this.className = className;
	}
	
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		this.access = access;
		if ((version&0xffff)<49) {
			// System.err.println("MethodInstrument:changing version from " + (version&0xffff) + "." + (version >> 16));
			version = 49;
		}
		super.visit(version, access, name, signature, superName, interfaces);
	}
	
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		if (!done && instrument() && instrument(name,access)) {
			return new MethodInstrumentMethod(access, name, desc, signature, exceptions, super.visitMethod(access,name,desc,signature,exceptions));
		} else {
			return super.visitMethod(access,name,desc,signature,exceptions);
		}
	}

	public void visitEnd() {
		if (!done && (access & Opcodes.ACC_INTERFACE) == 0) {
			done = true;
			try {
				Class k = Log.class;
				
				GeneratorAdapter mg;
				Label start;
				Label end;
				
				logBridgeMethod = new Method(LOG_INTERNAL_METHOD, LOG_INTERNAL_SIGNATURE);
				
				mg = new GeneratorAdapter(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, logBridgeMethod, LOG_INTERNAL_SIGNATURE, new Type[] {}, this);

				java.lang.reflect.Method m = k.getMethod(LOG_METHOD_NAME, String.class, String.class, String.class);

				start = mg.mark();
				mg.push(className);
				mg.loadArg(0);
				mg.loadArg(1);
				mg.invokeStatic(Type.getType(k), Method.getMethod(m));
				mg.returnValue();
				end   = mg.mark();
				mg.catchException(start, end, Type.getType(Throwable.class));
				mg.visitInsn(Opcodes.ICONST_0); // return false
				mg.returnValue();
				
				mg.endMethod();
				
				Set<String> keys = methods.keySet();
				
				for(String methName: keys) {
					makeMethod(methName, methods.get(methName));
				}
			} catch (NoSuchMethodException nsme) {
				System.err.println("Unable to find Log.reportMonitorEnter or Log.reportMonitorExit method");
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
		private int     access;
		private boolean added = false;
		
		MethodInstrumentMethod(int access, String name, String desc, String signature, String[] exceptions, MethodVisitor mv) {
			super(mv, access, name, desc);
			this.name = name;
			this.desc = desc;
			this.flagName = "$$$_"+(name+desc).replaceAll("[^\\p{Alnum}]","_");
			this.methName = "$$_"+(name+desc).replaceAll("[^\\p{Alnum}]","_");
			this.access = access;

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
