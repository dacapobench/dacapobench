package org.dacapo.instrument;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

public class Instrument {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		if (args.length != 4) System.exit(1);
		
		String infile             = args[0];
		String outfile            = args[1];
		String name               = args[2];
		String commandLineOptions = args[3];

		try {
			ClassReader reader = readClassFromFile(infile);
			
			ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);

			processOptions(writer, new Options(commandLineOptions));
			
			ClassVisitor cv = writer; 
			
			cv = new ClinitInstrument(cv, reader.getClassName()); // modify the <clinit> after the method instrumentation
			cv = new MethodInstrument(cv, reader.getClassName());
			cv = new RuntimeInstrument(cv);
			
			reader.accept(cv,ClassReader.EXPAND_FRAMES);
			
			writeClassToFile(writer,outfile);
		} catch (Exception e) {
			System.err.println("failed to process class "+name);
			System.err.println("exception "+e);
			e.printStackTrace();
			System.exit(1);
		}
	}

	private static void processOptions(ClassWriter writer, Options options) {
		
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
