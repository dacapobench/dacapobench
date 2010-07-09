package org.dacapo.instrument;

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;

public class VersionChanger extends ClassAdapter {

	public VersionChanger(ClassVisitor cv) {
		super(cv);
	}
	
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		if ((version&0xffff)<49) {
			version = 49;
		}
		super.visit(version, access, name, signature, superName, interfaces);
	}

}
