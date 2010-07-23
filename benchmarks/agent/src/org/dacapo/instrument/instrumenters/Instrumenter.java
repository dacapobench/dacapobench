package org.dacapo.instrument.instrumenters;

import java.io.File;
import java.io.InputStream;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;

import java.util.LinkedList;
import java.util.Properties;
import java.util.TreeMap;
import java.util.TreeSet;

import java.util.jar.JarInputStream;
import java.util.jar.JarEntry;

import org.dacapo.instrument.Agent;
import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class Instrumenter extends ClassAdapter {

	public static final String INSTRUMENT_PACKAGE = "org/dacapo/instrument/";
	
	public static final String PROP_CLASS_NAME = "class_name";
	
	@SuppressWarnings("unchecked")
	public static final Class  LOG_INTERNAL_CLASS = Agent.class;
	public static final Class  INSTRUMENTER_CLASS = Instrumenter.class;

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

	public static final String INTERNAL_LOG_PREFIX = "$$";
	public static final String INTERNAL_FIELD_PREFIX = "$$F$";
	public static final String INTERNAL_METHOD_PREFIX = "$$M$";
	
	public static final String INSTRUMENTER = INSTRUMENTER_CLASS.getName().replace('.','/');
	public static final String INSTRUMENTER_PACKAGE = INSTRUMENTER_CLASS.getPackage().getName().replace('.', '/');
	public static final String INSTRUMENTER_BASE = INSTRUMENTER.substring(INSTRUMENTER_PACKAGE.length() + 1);
	
	public static final String CLASS_SUFFIX = ".class";

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
		return method.startsWith(INTERNAL_LOG_PREFIX);
	}

}
