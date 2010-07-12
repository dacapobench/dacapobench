package org.dacapo.instrument;

import java.util.LinkedList;
import java.util.TreeMap;
import java.util.TreeSet;

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.LocalVariablesSorter;
import org.objectweb.asm.commons.Method;

public class SystemInstrument extends Instrument {

	private static final String   LOG_INTERNAL_NAME                = "org/dacapo/instrument/Log";
	
	private static final String   SYSTEM_GET_LOG_CLASS_METHOD      = "getLogClass";
	private static final String   SYSTEM_GET_LOG_CLASS_SIGNATURE   = "()Ljava/lang/Class;";

	private static final String   SYSTEM_LOG_CLASS_FIELD           = "logClass";
	
	private boolean doneAddField  = true;
	private boolean doneAddMethod = true;
	
	public SystemInstrument(ClassVisitor cv, TreeMap<String,Integer> methodToLargestLocal) {
		super(cv, methodToLargestLocal);
	}

	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		String systemClassName = System.class.getName().replace('.','/');
		
		if (systemClassName.equals(name)) {
			doneAddField   = false;
			doneAddMethod  = false;
		}
		
		super.visit(version, access, name, signature, superName, interfaces);
	}
	
	public void visitEnd() {
		if (! doneAddField) {
			doneAddField = true;
			
			super.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, SYSTEM_LOG_CLASS_FIELD, Type.getDescriptor(Log.class), null, null);
		}
		
		if (! doneAddMethod) {
			doneAddMethod = true;

			Class k = Log.class;
			
			GeneratorAdapter mg = new GeneratorAdapter(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, new Method(SYSTEM_GET_LOG_CLASS_METHOD, SYSTEM_GET_LOG_CLASS_SIGNATURE), SYSTEM_GET_LOG_CLASS_SIGNATURE, new Type[] {}, this);

			Label target = mg.newLabel();
			mg.getStatic(Type.getType(System.class), SYSTEM_LOG_CLASS_FIELD, Type.getType(Class.class));
			mg.ifNull(target);
			mg.push(Type.getType(Log.class));
			mg.putStatic(Type.getType(System.class), SYSTEM_LOG_CLASS_FIELD, Type.getType(Class.class));
			mg.mark(target);
			mg.getStatic(Type.getType(System.class), SYSTEM_LOG_CLASS_FIELD, Type.getType(Class.class));
			mg.returnValue();
		}
		
		super.visitEnd();
	}
}
