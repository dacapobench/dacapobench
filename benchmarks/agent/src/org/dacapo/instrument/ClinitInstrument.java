package org.dacapo.instrument;

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;

import org.objectweb.asm.Opcodes;

import org.objectweb.asm.commons.GeneratorAdapter;


public class ClinitInstrument  extends ClassAdapter {

	private static final int      CLINIT_ACCESS      = Opcodes.ACC_STATIC;
	private static final String   CLINIT_NAME        = "<clinit>";
	private static final String   CLINIT_DESCRIPTION = null;
	private static final String   CLINIT_SIGNATURE   = "()V";
	private static final String[] CLINIT_EXCEPTIONS  = { };
	
	private boolean             foundClinit = false;
	private ClassVisitor        cv          = null;
	
	public ClinitInstrument(ClassVisitor cv) {
		super(cv);
		this.cv = cv;
	}
	
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		foundClinit = false;
		super.visit(version, access, name, signature, superName, interfaces);
	}
	
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		if (CLINIT_NAME.equals(name) && CLINIT_SIGNATURE.equals(signature)) {
			System.out.print("found Clinit[" + Opcodes.ACC_STATIC + "]: " + access + ", " + name + ", " + desc + ", " + signature + ", {");
			if (exceptions != null && 0<exceptions.length) {
				System.out.print(" "+exceptions[0]);
				for(int i=1; i<exceptions.length; i++)
					System.out.print(", "+exceptions[i]);
			}
			System.out.println(" }");
			
			foundClinit = true;
			return new ClinitInstrumentMethod(super.visitMethod(access,name,desc,signature,exceptions));
		} else
			return super.visitMethod(access,name,desc,signature,exceptions);
	}
	
	public void visitEnd() {
		if (!foundClinit) {
			System.out.println("Must add a <clinit>");

			// add <clinit> method here
			// cv.visitMethod()
		}
		
		super.visitEnd();
	}
	
	private class ClinitInstrumentMethod extends MethodAdapter {
		private MethodVisitor mv;
		ClinitInstrumentMethod(MethodVisitor mv) {
			super(mv);
			this.mv= mv;
		}
	}
}
