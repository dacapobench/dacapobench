package org.dacapo.instrument.instrumenters;

import java.util.Properties;
import java.util.TreeMap;

import org.dacapo.instrument.Agent;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

public class SystemInstrument extends Instrumenter {

	public static final Class[]   DEPENDENCIES = new Class[] { RuntimeInstrument.class };

	private static final String   LOG_CLASS_METHOD      = "getLogClass";
	private static final String   LOG_CLASS_SIGNATURE   = Type.getMethodDescriptor(JAVA_LANG_CLASS_TYPE, new Type[0]);

	private static final String   CLASS_FIELD           = "logClass";
	
	private boolean doneAddField  = true;
	private boolean doneAddMethod = true;
	
	public static ClassVisitor make(ClassVisitor cv, TreeMap<String,Integer> methodToLargestLocal,
			Properties options, Properties state) {
		if (options.containsKey("TODO"))
			cv = new SystemInstrument(cv, methodToLargestLocal, options, state);
		return cv;
	}
	
	protected SystemInstrument(ClassVisitor cv, TreeMap<String,Integer> methodToLargestLocal,
			Properties options, Properties state) {
		super(cv, methodToLargestLocal, options, state);
	}

	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		if (JAVA_LANG_SYSTEM.equals(name)) {
			doneAddField   = false;
			doneAddMethod  = false;
		}
		
		super.visit(version, access, name, signature, superName, interfaces);
	}
	
	public void visitEnd() {
		if (! doneAddField) {
			doneAddField = true;
			
			super.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, CLASS_FIELD, Type.getDescriptor(Agent.class), null, null);
		}
		
		if (! doneAddMethod) {
			doneAddMethod = true;

			GeneratorAdapter mg = new GeneratorAdapter(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, new Method(LOG_CLASS_METHOD, LOG_CLASS_SIGNATURE), LOG_CLASS_SIGNATURE, new Type[] {}, this);

			Label target = mg.newLabel();
			mg.getStatic(JAVA_LANG_SYSTEM_TYPE, CLASS_FIELD, JAVA_LANG_CLASS_TYPE);
			mg.ifNull(target);
			mg.push(LOG_INTERNAL_TYPE);
			mg.putStatic(JAVA_LANG_SYSTEM_TYPE, CLASS_FIELD, JAVA_LANG_CLASS_TYPE);
			mg.mark(target);
			mg.getStatic(JAVA_LANG_SYSTEM_TYPE, CLASS_FIELD, JAVA_LANG_CLASS_TYPE);
			mg.returnValue();
		}
		
		super.visitEnd();
	}
}
