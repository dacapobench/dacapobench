package org.dacapo.instrument.instrumenters;

import java.util.Properties;
import java.util.TreeMap;

import org.objectweb.asm.ClassVisitor;

public class VersionChanger extends Instrumenter {

	public static final Class[] DEPENDENCIES = new Class[] { };

	public static ClassVisitor make(ClassVisitor cv, TreeMap<String,Integer> methodToLargestLocal, Properties options, Properties state) {
		return new VersionChanger(cv, methodToLargestLocal, options, state); 
	}
	
	private VersionChanger(ClassVisitor cv, TreeMap<String,Integer> methodToLargestLocal, Properties options, Properties state) {
		super(cv, methodToLargestLocal, options, state);
	}
	
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		if ((version&0xffff)<49) {
			version = 49;
		}
		super.visit(version, access, name, signature, superName, interfaces);
	}

}
