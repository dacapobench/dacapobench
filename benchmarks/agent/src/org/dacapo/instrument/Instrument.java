package org.dacapo.instrument;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URI;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;

import org.dacapo.instrument.instrumenters.AllocateInstrument;
import org.dacapo.instrument.instrumenters.CallChainInstrument;
import org.dacapo.instrument.instrumenters.ClinitInstrument;
import org.dacapo.instrument.instrumenters.Instrumenter;
import org.dacapo.instrument.instrumenters.InstrumenterFactory;
import org.dacapo.instrument.instrumenters.LogInstrument;
import org.dacapo.instrument.instrumenters.MethodInstrument;
import org.dacapo.instrument.instrumenters.MonitorInstrument;
import org.dacapo.instrument.instrumenters.RuntimeInstrument;
import org.dacapo.instrument.instrumenters.VersionChanger;
import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import org.objectweb.asm.commons.AdviceAdapter;

public class Instrument extends Instrumenter {

	public Instrument(ClassVisitor arg0, TreeMap<String,Integer> methodToLargestLocal, Properties options, Properties state) {
		super(arg0, methodToLargestLocal, options, state);
	}

	private final static String CONFIG_FILE_NAME = "config.txt";
	private final static String STATE_FILE_NAME = "state.txt";
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		if (args.length < 4) System.exit(1);
		
		String infile             = args[0];
		String outfile            = args[1];
		String name               = args[2];
		// String commandLineOptions = args[3];
		File   agentDir           = new File(args[3]).getAbsoluteFile();
		
		if (!agentDir.exists() || !agentDir.isDirectory()) {
			System.err.println("agentDir does not exist or is not a directory");
			System.exit(10);
		}

		try {
			TreeMap<String,Integer> methodToLargestLocal = new TreeMap<String,Integer>();

			// read the class to do some pre-processing
			ClassReader reader = readClassFromFile(infile);
			
			//
			ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);

			ClassVisitor cv = writer; 

			// make a map of local variable sizes for later use,
			// this is to get around either a short coming or 
			cv = new LocalSizes(writer, methodToLargestLocal);
			
			reader.accept(cv,ClassReader.EXPAND_FRAMES);

			reader = readClassFromFile(infile);

			cv = writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
			
			Properties options = new Properties();
			if (! readProperties(options, new File(agentDir, CONFIG_FILE_NAME))) {
				System.err.println("Must specify an agent working directory");
				System.exit(10);
			}
			options.setProperty(PROP_CLASS_NAME,reader.getClassName());

			// make a state property
			Properties state = new Properties();
			
			// read state properties from the previous run
			readProperties(state, new File(agentDir, STATE_FILE_NAME));

			// make the transform chain
			cv = InstrumenterFactory.makeTransformChain(cv, methodToLargestLocal, options, state);
			
			// transform the class
			reader.accept(cv,ClassReader.EXPAND_FRAMES);
			
			// write the transformed class
			writeClassToFile(writer,outfile);

			// save the state
			writeProperties(state, new File(agentDir, STATE_FILE_NAME));
		} catch (Exception e) {
			System.err.println("failed to process class "+name);
			System.err.println("exception "+e);
			e.printStackTrace();
			System.exit(1);
		}
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

	private static boolean readProperties(Properties prop, File file) {
		if (!file.exists() || !file.canRead())
			return false;

		try {
			FileInputStream is = new FileInputStream(file);
			try {
				prop.load(is);
				return true;
			} finally {
				is.close();
			}
		} catch (Throwable t) {
			return false;
		}
	}

	private static boolean writeProperties(Properties prop, File file) {
		if (file.exists() && !file.canWrite())
			return false;

		try {
			PrintStream os = new PrintStream(file);
			try {
				prop.store(os, "properties");
				return true;
			} finally {
				os.close();
			}
		} catch (Throwable t) {
			return false;
		}
	}
}
