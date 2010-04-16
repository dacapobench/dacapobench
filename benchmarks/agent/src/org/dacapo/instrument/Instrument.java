package org.dacapo.instrument;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

public class Instrument {

	private static Options options = null;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		if (args.length < 4) System.exit(1);
		
		String infile             = args[0];
		String outfile            = args[1];
		String name               = args[2];
		String commandLineOptions = args[3];
		
		for(int i=4; i<args.length; i++) {
			commandLineOptions = commandLineOptions+" "+args[i];
		}

		try {
			ClassReader reader = readClassFromFile(infile);
			
			ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);

			options = new Options(commandLineOptions);
			
			ClassVisitor cv = writer; 
			
			if (options.has(Options.RUNTIME))
				cv = new RuntimeInstrument(cv);
			
			if (options.has(Options.MONITOR))
				cv = new MonitorInstrument(cv);
			
			if (options.has(Options.CLASSES_INITIALIZATION))
				cv = new ClinitInstrument(cv, reader.getClassName()); 
			
			if (options.has(Options.ALLOCATE))
				cv = new AllocateInstrument(cv);
			
			if (options.has(Options.CALL_CHAIN))
				cv = new CallChainInstrument(cv);
			
			// The MethodInstrument is left out as there are a number of issues with 
			// instrumenting the bootclasses that I have not been able to resolve.
			//
			if (options.has(Options.METHOD_INSTR))
				cv = new MethodInstrument(cv, reader.getClassName());
			
			if (options.has(Options.LOG_START)) {
				String startMethod = options.value(Options.LOG_START);
				String stopMethod  = options.value(Options.LOG_STOP);
				
				if (stopMethod != null) {
					cv = new LogInstrument(cv, startMethod, stopMethod);
				} else {
					cv = new LogInstrument(cv, startMethod);
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

}
