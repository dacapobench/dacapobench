package org.dacapo.instrument.instrumenters;

import java.util.Properties;
import java.util.TreeMap;

import org.dacapo.instrument.Configuration;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

public class RuntimeInstrument extends Instrumenter {
	
	public static final Class[]   DEPENDENCIES = new Class[] { VersionChanger.class };

	public static final String    RUNTIME                      = "runtime";

	private static final Type     JAVA_LANG_RUNTIME_TYPE       = Type.getType(Runtime.class);
	private static final String   JAVA_LANG_RUNTIME            = JAVA_LANG_RUNTIME_TYPE.getInternalName();
	private static final String   RUNTIME_METHOD_NAME          = "availableProcessors";
	private static final String   RUNTIME_SIGNATURE            = Type.getMethodDescriptor(Type.INT_TYPE, new Type[0]);

	private static final Type     CONFIGURATION_TYPE           = Type.getType(Configuration.class);
	
	private static final String   CONFIGURATION                = CONFIGURATION_TYPE.getInternalName();
	private static final String   CONFIGURATION_METHOD_NAME    = RUNTIME_METHOD_NAME;
	private static final String   CONFIGURATION_SIGNATURE      = Type.getMethodDescriptor(Type.INT_TYPE, new Type[] { JAVA_LANG_RUNTIME_TYPE });
	private static final String   CONFIGURATION_FIELD_NAME     = "processorCount";

	private static final String   RUNTIME_INTERNAL_METHOD_NAME = INTERNAL_LOG_PREFIX + CONFIGURATION_METHOD_NAME;
	private static final String   RUNTIME_INTERNAL_SIGNATURE   = CONFIGURATION_SIGNATURE;
	
	private boolean             done                           = false;
	private boolean             found                          = false;
	private String              name;
	private int                 access;
	private boolean             inherit;
	private boolean             overridden                     = false;

	public static ClassVisitor make(ClassVisitor cv, TreeMap<String,Integer> methodToLargestLocal, Properties options, Properties state) {
		if (options.containsKey(RUNTIME))
			cv = new RuntimeInstrument(cv, methodToLargestLocal, options, state);
		return cv;
	}
	
	private RuntimeInstrument(ClassVisitor cv, TreeMap<String,Integer> methodToLargestLocal, Properties options, Properties state) {
		super(cv, methodToLargestLocal, options, state);
	}

	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		this.name       = name;
		this.access     = access;
		this.inherit    = superName!=null?JAVA_LANG_RUNTIME.equals(superName.replace('.', '/')):false;
		super.visit(version, access, name, signature, superName, interfaces);
	}
		
	// find and transform all invokevirtual 
	//   java.lang.Runtime.availableProcessors()I;
	// into
	//   org.dacapo.instrument.Configuration.availableProcessors(Ljava/lang/Runtime;)I;
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		MethodVisitor mv = super.visitMethod(access,name,desc,signature,exceptions);
		if (done)
			return mv;
		else if (inherit && RUNTIME_METHOD_NAME.equals(name) && RUNTIME_SIGNATURE.equals(signature) && instrument(access))
			return new InheritInstrumentMethod(mv);
		else if (instrument() && !isGenerated(name) && instrument(access))
			return new RuntimeInstrumentMethod(mv);
		else
			return mv;
	}
	
	public void visitEnd() {
		if (!done) {
			done = true;
			if (inherit && !overridden) {
				// add method to retrieve processor count that overrides the parent method.
				GeneratorAdapter mg;
				Method m = new Method(RUNTIME_METHOD_NAME, RUNTIME_SIGNATURE);
				
				mg = new GeneratorAdapter(Opcodes.ACC_PUBLIC, m, RUNTIME_SIGNATURE, new Type[] {}, this);

				Label start = mg.mark();
				// attempt to invoke
			    mg.invokeStatic(CONFIGURATION_TYPE, new Method(CONFIGURATION_METHOD_NAME, CONFIGURATION_SIGNATURE));
				mg.returnValue();
				Label end = mg.mark();

				mg.catchException(start, end, JAVA_LANG_THROWABLE_TYPE);
				
				mg.loadArg(0);
				mg.invokeVirtual(JAVA_LANG_RUNTIME_TYPE, m);
				mg.returnValue();

				mg.endMethod();

			}
			
			if (found) {
				// add method $$availableProcessors to retrieve the availableProcessors from Configuration if it can.
				GeneratorAdapter mg;
				
				mg = new GeneratorAdapter(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, new Method(RUNTIME_INTERNAL_METHOD_NAME, RUNTIME_INTERNAL_SIGNATURE), RUNTIME_INTERNAL_SIGNATURE, new Type[] {}, this);

				Label start = mg.mark();
				// attempt to invoke
				mg.loadArg(0);
			    mg.invokeStatic(CONFIGURATION_TYPE, new Method(CONFIGURATION_METHOD_NAME, CONFIGURATION_SIGNATURE));
				mg.returnValue();
				Label end = mg.mark();

				mg.catchException(start, end, JAVA_LANG_THROWABLE_TYPE);
				
				mg.loadArg(0);
				mg.invokeVirtual(JAVA_LANG_RUNTIME_TYPE, new Method(RUNTIME_METHOD_NAME, RUNTIME_SIGNATURE));
				mg.returnValue();

				mg.endMethod();
				
			}
		}
		super.visitEnd();
	}
	
	private boolean instrument() {
		return (access & Opcodes.ACC_INTERFACE) == 0;
	}
	
	private boolean instrument(int access) {
		return (access & Opcodes.ACC_ABSTRACT) == 0 && (access & Opcodes.ACC_BRIDGE) == 0 && (access & Opcodes.ACC_NATIVE) == 0;
	}
	
	private class InheritInstrumentMethod extends MethodAdapter {
		InheritInstrumentMethod(MethodVisitor mv) {
			super(mv);
		}

		@SuppressWarnings("unused")
		protected void onMethodEnter() {
			if (done) return;

			overridden = true;
			Label start  = new Label();
			Label normal = new Label();
			super.visitLabel(start);
			super.visitFieldInsn(Opcodes.GETSTATIC, CONFIGURATION, CONFIGURATION_FIELD_NAME, Type.INT_TYPE.getDescriptor());
			super.visitInsn(Opcodes.DUP);
			super.visitJumpInsn(Opcodes.IFEQ, normal);
			super.visitInsn(Opcodes.IRETURN);
			super.visitLabel(normal);
			super.visitInsn(Opcodes.POP);
			Label end = new Label();
			super.visitJumpInsn(Opcodes.GOTO, end);
			super.visitLabel(end);
			super.visitTryCatchBlock(start, normal, end, Type.getType(Throwable.class).getDescriptor());
		}
}
	
	private class RuntimeInstrumentMethod extends MethodAdapter {
		RuntimeInstrumentMethod(MethodVisitor mv) {
			super(mv);
			this.mv = mv;
		}
		
		public void visitMethodInsn(int opcode, String owner, String methodName, String desc) {
			if ((JAVA_LANG_RUNTIME.equals(owner) || (inherit && name.equals(owner))) &&
				RUNTIME_METHOD_NAME.equals(methodName) &&
				RUNTIME_SIGNATURE.equals(desc)) {
				found = true;
				super.visitMethodInsn(Opcodes.INVOKESTATIC,name,RUNTIME_INTERNAL_METHOD_NAME,RUNTIME_INTERNAL_SIGNATURE);
			} else {
				super.visitMethodInsn(opcode,owner,methodName,desc);
			}
		}
	}
}
