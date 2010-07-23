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

public class CallChainInstrument extends Instrumenter {

	public static final Class[]   DEPENDENCIES = new Class[] { AllocateInstrument.class };

	public static final String    CALL_CHAIN             = "call_chain";

	private static final String   LOG_METHOD_NAME        = "logCallChain";
	private static final String   LOG_METHOD_SIGNATURE   = Type.getMethodDescriptor(Type.VOID_TYPE, new Type[0]); 
	
	private static final String   LOG_INTERNAL_METHOD    = "$$" + LOG_METHOD_NAME;
	
	private int                   access      = 0;
	private String                className   = null;
	private boolean               done        = false;
	private boolean               found       = false;
	
	public static ClassVisitor make(ClassVisitor cv, TreeMap<String,Integer> methodToLargestLocal,
			Properties options, Properties state) {
		if (options.containsKey(CALL_CHAIN))
			cv = new CallChainInstrument(cv, methodToLargestLocal, options, state);
		return cv;
	}
	
	protected CallChainInstrument(ClassVisitor cv, TreeMap<String,Integer> methodToLargestLocal,
			Properties options, Properties state) {
		super(cv, methodToLargestLocal, options, state);
	}
	
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		this.access    = access;
		this.className = name;
		super.visit(version, access, name, signature, superName, interfaces);
	}
	
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		if (!done && instrument() && !isGenerated(name) && instrument(name,access)) {
			return new CallChainInstrumentMethod(access, name, desc, signature, exceptions, super.visitMethod(access,name,desc,signature,exceptions));
		} else {
			return super.visitMethod(access,name,desc,signature,exceptions);
		}
	}

	@SuppressWarnings("unchecked")
	public void visitEnd() {
		if (!done && found) {
			done = true;
			try {
				GeneratorAdapter mg = new GeneratorAdapter(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, new Method(LOG_INTERNAL_METHOD, LOG_METHOD_SIGNATURE), LOG_METHOD_SIGNATURE, new Type[] {}, this);

				Label start = mg.mark();
				mg.invokeStatic(LOG_INTERNAL_TYPE, Method.getMethod(LOG_INTERNAL_CLASS.getMethod(LOG_METHOD_NAME)));
				mg.returnValue();
				Label end   = mg.mark();
				mg.catchException(start, end, JAVA_LANG_THROWABLE_TYPE);
				mg.returnValue();
				
				mg.endMethod();
			} catch (NoSuchMethodException nsme) {
				System.err.println("Unable to find Agent.rlogCallChain method");
				System.err.println("M:"+nsme);
				nsme.printStackTrace();
			}
		}
		
		super.visitEnd();
	}
	
	private boolean instrument() {
		return (access & Opcodes.ACC_INTERFACE) == 0 && !className.startsWith(INSTRUMENT_PACKAGE);
	}

	private boolean instrument(String name, int access) {
		return (access & Opcodes.ACC_ABSTRACT) == 0 && (access & Opcodes.ACC_BRIDGE) == 0 && (access & Opcodes.ACC_NATIVE) == 0;
	}
	
	private class CallChainInstrumentMethod extends AdviceAdapter {
		CallChainInstrumentMethod(int access, String name, String desc, String signature, String[] exceptions, MethodVisitor mv) {
			super(mv, access, name, desc);
		}
		
		protected void onMethodEnter() {
			if (done) return;

			found = true;
			
			super.visitMethodInsn(Opcodes.INVOKESTATIC, className, LOG_INTERNAL_METHOD, LOG_METHOD_SIGNATURE);
		}
	}
	
}
