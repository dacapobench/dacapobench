package org.dacapo.transform;

import java.io.FileInputStream;

import org.objectweb.asm.ClassReader;

public class DumpClass {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		if (args.length != 4) System.exit(1);
		
		String infile  = args[0];
		String outfile = args[1];
		String name    = args[2];
		String options = args[3];

		ClassReader reader = new ClassReader(new FileInputStream(infile));
		
		System.out.println("DumpClass: class name="+reader.getClassName());
		
		System.exit(1);
	}

}
