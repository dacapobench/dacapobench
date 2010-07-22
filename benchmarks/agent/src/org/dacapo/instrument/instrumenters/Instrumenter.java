package org.dacapo.instrument.instrumenters;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;
import java.util.TreeMap;

import org.dacapo.instrument.Agent;
import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import org.objectweb.asm.commons.AdviceAdapter;

public class Instrumenter extends ClassAdapter {

	public static final String INSTRUMENT_PACKAGE = "org/dacapo/instrument/";
	
	public static final String PROP_CLASS_NAME = "class_name";
	
	@SuppressWarnings("unchecked")
	public static final Class  LOG_INTERNAL_CLASS = Agent.class;

	public static final Type   JAVA_LANG_OBJECT_TYPE = Type.getType(Object.class);
	public static final Type   JAVA_LANG_CLASS_TYPE = Type.getType(Class.class);
	public static final Type   JAVA_LANG_STRING_TYPE = Type.getType(String.class);
	public static final Type   JAVA_LANG_SYSTEM_TYPE = Type.getType(System.class);
	public static final Type   JAVA_LANG_THROWABLE_TYPE = Type.getType(Throwable.class);
	public static final Type   LOG_INTERNAL_TYPE = Type.getType(LOG_INTERNAL_CLASS);
	
	public static final String JAVA_LANG_OBJECT = JAVA_LANG_OBJECT_TYPE.getInternalName();
	public static final String JAVA_LANG_CLASS = JAVA_LANG_CLASS_TYPE.getInternalName();
	public static final String JAVA_LANG_STRING = JAVA_LANG_STRING_TYPE.getInternalName();
	public static final String JAVA_LANG_SYSTEM = JAVA_LANG_SYSTEM_TYPE.getInternalName();
	public static final String JAVA_LANG_THROWABLE = JAVA_LANG_THROWABLE_TYPE.getInternalName();
	public static final String LOG_INTERNAL = LOG_INTERNAL_TYPE.getInternalName();

	public static final String INTERNAL_PREFIX = "$$";
	
	protected TreeMap<String,Integer> methodToLargestLocal;
	protected Properties options = null;
	protected Properties state = null; 

	public Instrumenter(ClassVisitor arg0, TreeMap<String,Integer> methodToLargestLocal, Properties options, Properties state) {
		super(arg0);
		
		this.methodToLargestLocal = methodToLargestLocal;
		this.state = state;
		this.options = options;
	}
	
	protected static String encodeMethodName(String klass, String method, String signature) {
		return klass+"."+method+signature;
	}
	
	protected static int getArgumentSizes(int access, String desc) {
		return 
			(((access & Opcodes.ACC_STATIC) == 0)?1:0) +		
			Type.getArgumentsAndReturnSizes(desc) >> 2; 
	}
	
	protected static boolean isGenerated(String method) {
		return method.startsWith(INTERNAL_PREFIX);
	}
}