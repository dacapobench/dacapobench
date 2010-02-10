package org.dacapo.instrument;

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;

public class RuntimeInstrument extends ClassAdapter {
	
	private ClassVisitor        cv          = null;
	
	public RuntimeInstrument(ClassVisitor cv) {
		super(cv);
		this.cv = cv;
	}

	// find and transform all invokevirtual 
	//   java.lang.Runtime.availableProcessors()I;
	// into
	//   org.dacapo.instrument.Configuration.availableProcessors(Ljava/lang/Runtime;)I;
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		return new RuntimeInstrumentMethod(super.visitMethod(access,name,desc,signature,exceptions));
	}
	
	private class RuntimeInstrumentMethod extends MethodAdapter {
		private MethodVisitor mv;
		RuntimeInstrumentMethod(MethodVisitor mv) {
			super(mv);
			this.mv = mv;
		}
	}
}
