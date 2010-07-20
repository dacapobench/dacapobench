package org.dacapo.instrument;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.TreeMap;

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import org.objectweb.asm.commons.AdviceAdapter;

public class Instrument extends ClassAdapter {

	protected TreeMap<String,Integer> methodToLargestLocal;
	
	public Instrument(ClassVisitor arg0, TreeMap<String,Integer> methodToLargestLocal) {
		super(arg0);
		
		this.methodToLargestLocal = methodToLargestLocal;
	}

	private static Options options = null;

	private final static String CONFIG_FILE_NAME = "config.txt";
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		if (args.length < 4) System.exit(1);
		
		String infile             = args[0];
		String outfile            = args[1];
		String name               = args[2];
		// String commandLineOptions = args[3];
		String agentDir           = args[3];
		
//		for(int i=4; i<args.length; i++) {
//			commandLineOptions = commandLineOptions+","+args[i];
//		}

		try {
			TreeMap<String,Integer> methodToLargestLocal = new TreeMap<String,Integer>();
			
			ClassReader reader = readClassFromFile(infile);
			
			ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);

			ClassVisitor cv = writer; 

			cv = new LocalSizes(writer, methodToLargestLocal);
			
			reader.accept(cv,ClassReader.EXPAND_FRAMES);

			reader = readClassFromFile(infile);
			
			cv = writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
			
			// options = new Options(commandLineOptions);
			options = new Options(new FileInputStream(new File(agentDir, CONFIG_FILE_NAME)));
			
			// always change the version of the class to allow  SomeClass.class constants
			cv = new VersionChanger(cv);
			
			// Unable to 
			// cv = new SystemInstrument(cv, methodToLargestLocal);
			
			if (options.has(Options.RUNTIME))
				cv = new RuntimeInstrument(cv, methodToLargestLocal);
			
			if (options.has(Options.MONITOR))
				cv = new MonitorInstrument(cv, methodToLargestLocal);
			
			if (options.has(Options.CLASSES_INITIALIZATION))
				cv = new ClinitInstrument(cv, methodToLargestLocal, options.value(Options.CLASSES_INITIALIZATION)); 
			
			if (options.has(Options.ALLOCATE) || options.has(Options.POINTER))
				cv = new AllocateInstrument(cv, methodToLargestLocal, options.value(Options.ALLOCATE), options.has(Options.POINTER));
			
			if (options.has(Options.CALL_CHAIN))
				cv = new CallChainInstrument(cv, methodToLargestLocal);
			
			// The MethodInstrument is left out as there are a number of issues with 
			// instrumenting the bootclasses that I have not been able to resolve.
			//
			if (options.has(Options.METHOD_INSTR))
				cv = new MethodInstrument(cv, methodToLargestLocal, reader.getClassName());
			
			if (options.has(Options.LOG_START)) {
				String startMethod = options.value(Options.LOG_START);
				String stopMethod  = options.value(Options.LOG_STOP);
				
				if (stopMethod != null) {
					cv = new LogInstrument(cv, methodToLargestLocal, startMethod, stopMethod);
				} else {
					cv = new LogInstrument(cv, methodToLargestLocal, startMethod);
				}
			}
			
			reader.accept(cv,ClassReader.EXPAND_FRAMES);
			
			writeClassToFile(writer,outfile);
		} catch (Exception e) {
			System.err.println("failed to process class "+name);
			System.err.println("exception "+e);
			e.printStackTrace();
			System.exit(1);
		}
	}

	static int getArgumentSizes(int access, String desc) {
		return 
			(((access & Opcodes.ACC_STATIC) == 0)?1:0) +		
			Type.getArgumentsAndReturnSizes(desc) >> 2; 
	}
	
	private static ClassReader readClassFromFile(String infile) throws Exception {
		FileInputStream is = null;
		try {
			is = new FileInputStream(infile);
			ClassReader reader = new ClassReader(is);
			return reader;
		} finally {
			if (is != null) is.close();
		}
	}
	
	private static void writeClassToFile(ClassWriter writer, String outfile) throws Exception {
		FileOutputStream os = null;
		try {
			File oFile = new File(outfile);
			oFile.delete();
			os = new FileOutputStream(oFile);
			os.write(writer.toByteArray());
		} finally {
			if (os != null) os.close();
		}
	}

	static String encodeMethodName(String klass, String method, String signature) {
		return klass+"."+method+signature;
	}
	
	private static class LocalSizes extends ClassAdapter {
		private TreeMap<String,Integer> methodToLargestLocal;
		
		private boolean check; 
		private String name;
		
		public LocalSizes(ClassVisitor cv, TreeMap<String,Integer> methodToLargestLocal) {
			super(cv);
			this.methodToLargestLocal = methodToLargestLocal;
		}
		
		public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
			this.name   = name;
			this.check  = (access & Opcodes.ACC_INTERFACE) == 0;
		}
		public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
			if (check && (access & (Opcodes.ACC_NATIVE | Opcodes.ACC_ABSTRACT))==0)
				return new Maxs(access, this.name, name, desc, super.visitMethod(access, name, desc, signature, exceptions), methodToLargestLocal);
			else
				return super.visitMethod(access, name, desc, signature, exceptions);
		}
	}
	
	private static class Maxs extends AdviceAdapter {
		public String encodedName;
		public int    maxLocals;
		public int    maxStack;
		private TreeMap<String,Integer> methodToLargestLocal;
		
		public Maxs(int access, String klass, String name, String desc, MethodVisitor mv, TreeMap<String,Integer> methodToLargestLocal) {
			super(mv, access, name, desc);
			
			this.encodedName = encodeMethodName(klass,name,desc);
			this.maxLocals = getArgumentSizes(access, desc);
			this.maxStack  = 0; 
			this.methodToLargestLocal = methodToLargestLocal;
			
			this.methodToLargestLocal.put(this.encodedName, new Integer(this.maxLocals));
		}
		
		public void visitVarInsn(int opcode, int var) {
			if (this.maxLocals < var) {
				this.maxLocals = var;
				methodToLargestLocal.put(this.encodedName, new Integer(this.maxLocals));
			}
			super.visitVarInsn(opcode, var);
		}
		
		public void visitMaxs(int maxStack, int maxLocals) {
			this.maxLocals = maxLocals;
			if (this.maxLocals < maxStack) {
				this.maxStack  = maxStack;
				methodToLargestLocal.put(encodedName, new Integer(this.maxLocals));
			}
			super.visitMaxs(maxStack, maxLocals);
		}
	}
}
