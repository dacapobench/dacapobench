package org.dacapo.instrument.instrumenters;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.LinkedList;
import java.util.Properties;
import java.util.TreeMap;
import java.util.TreeSet;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

public class AllocateInstrument extends Instrumenter {

	public static final Class[] DEPENDENCIES = new Class[] { ClinitInstrument.class };

	// we need to instrument a call to alloc in the constructor, after the super
	// class init but before the other code. This should call the reportAlloc
	// if the type of this class is the same as the class containing the
	// constructor.
	// if this.getClass() == Class.class then report allocate

	// instrument reference changes from
	// putfield ...,obj,v' => ...
	// to
	// dup2 ...,obj,v' => ...,obj,v',obj,v'
	// swap ...,obj,v',obj,v' => ...,obj,v',v',obj
	// dup ...,obj,v',v',obj => ...,obj,v',v',obj,obj
	// getfield ...,obj,v',v',obj,obj => ...,obj,v',v',obj,v
	// invokespecial
	// pointerchangelog(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V
	// ...,obj,v',v',obj,v => ...,obj,v'
	// putfield ...,obj,v' =>

	public static final String ALLOCATE               = "allocate";
	public static final String POINTER                = "pointer";

	private static final String LOG_ALLOC_INC = "allocInc";
	private static final String LOG_ALLOC_DEC = "allocDec";
	private static final String LOG_ALLOC_DONE = "allocDone";
	private static final String LOG_ALLOC_REPORT = "allocReport";
	private static final String LOG_POINTER_CHANGE = "logPointerChange";
	private static final String LOG_STATIC_POINTER_CHANGE = "logStaticPointerChange";

	private static final String LOG_INTERNAL_ALLOC_INC = INTERNAL_LOG_PREFIX
			+ LOG_ALLOC_INC;
	private static final String LOG_INTERNAL_ALLOC_DEC = INTERNAL_LOG_PREFIX
			+ LOG_ALLOC_DEC;
	private static final String LOG_INTERNAL_ALLOC_DONE = INTERNAL_LOG_PREFIX
			+ LOG_ALLOC_DONE;
	private static final String LOG_INTERNAL_ALLOC_REPORT = INTERNAL_LOG_PREFIX
			+ LOG_ALLOC_REPORT;
	private static final String LOG_INTERNAL_POINTER_CHANGE = INTERNAL_LOG_PREFIX
			+ LOG_POINTER_CHANGE;
	private static final String LOG_INTERNAL_STATIC_POINTER_CHANGE = INTERNAL_LOG_PREFIX
			+ LOG_STATIC_POINTER_CHANGE;

	private static final String VOID_SIGNATURE = Type.getMethodDescriptor(Type.VOID_TYPE, new Type[0]); // "()V";
	private static final String OBJECT_SIGNATURE = Type.getMethodDescriptor(Type.VOID_TYPE, new Type[] { JAVA_LANG_OBJECT_TYPE }); // "(Ljava/lang/Object;)V";
	private static final String OBJECT_SITE_SIGNATURE = Type.getMethodDescriptor(Type.VOID_TYPE, new Type[] { JAVA_LANG_OBJECT_TYPE, Type.INT_TYPE });
	private static final String POINTER_CHANGE_SIGNATURE = 
		Type.getMethodDescriptor(Type.VOID_TYPE, new Type[] { JAVA_LANG_OBJECT_TYPE, JAVA_LANG_OBJECT_TYPE, JAVA_LANG_OBJECT_TYPE });
	private static final String STATIC_POINTER_CHANGE_SIGNATURE =
		Type.getMethodDescriptor(Type.VOID_TYPE, new Type[] { JAVA_LANG_OBJECT_TYPE, JAVA_LANG_CLASS_TYPE, JAVA_LANG_OBJECT_TYPE });

	private static final String JAVA_LANG_CONSTRUCTOR = Type.getInternalName(java.lang.reflect.Constructor.class);
	private static final String NEW_INSTANCE = "newInstance";
	
	private static final String ALLOCATE_BASE_LOG_FILE_NAME = "allocate.csv";
	private static final String PROP_SITE_ID = "allocate.instrument.site.id";

	private String name;
	private String superName;
	private int access;
	private boolean done = false;
	private boolean containsAllocate = false;
	private boolean hasConstructor = false;
	private boolean logPointerChange = false;
	private TreeSet<String> finalFields = new TreeSet<String>();
	
	private LinkedList<String> excludePackages = new LinkedList<String>();
	
	private String  allocateLogBaseName = null;
	
	private static class Triplet {
		public String type;
		public int var;
		public int siteId;
	}

	private static class CountLocals extends MethodAdapter {
		int max;

		public CountLocals(int access, MethodVisitor mv) {
			super(mv);
			max = ((access & Opcodes.ACC_STATIC) != 0) ? -1 : 0;
		}

		public int getMaxLocals() {
			super.visitCode();
			return max;
		}

		public void visitVarInsn(int opcode, int var) {
			super.visitVarInsn(opcode, var);
			max = Math.max(max, var);
		}
	};

	public static ClassVisitor make(ClassVisitor cv, TreeMap<String, Integer> methodToLargestLocal,
			Properties options, Properties state) {
		if (options.containsKey(ALLOCATE) || options.containsKey(POINTER)) {
			cv = new AllocateInstrument(cv, methodToLargestLocal, options, state, options.getProperty(ALLOCATE), options.containsKey(POINTER));
		}
		return cv;
	}
	
	protected AllocateInstrument(ClassVisitor cv,
			TreeMap<String, Integer> methodToLargestLocal,
			Properties options, Properties state,
			String excludeList,
			boolean logPointerChange) {
		super(cv, methodToLargestLocal, options, state);
		this.logPointerChange = logPointerChange;

		// always add the instrument package to the exclude list
		excludePackages.add(INSTRUMENT_PACKAGE);
		if (excludeList != null) {
			String[] packageList = excludeList.split(";");
			for (String p : packageList) {
				p = p.replace('.', '/');
				if (!p.endsWith("/"))
					p += p + "/";
				excludePackages.add(p);
			}
		}
		
		String path = options.getProperty(PROP_AGENT_DIRECTORY);
		if (path!=null) {
			File agentPath = new File(path);
			File log = new File(agentPath,ALLOCATE_BASE_LOG_FILE_NAME);
			if (agentPath.exists() && agentPath.canWrite()) {
				if (!log.exists() || (log.exists() && log.canWrite()))
					this.allocateLogBaseName = log.getAbsolutePath();
			}
		}
	}

	public void visit(int version, int access, String name, String signature,
			String superName, String[] interfaces) {
		this.name = name;
		this.access = access;
		this.superName = superName;
		super.visit(version, access, name, signature, superName, interfaces);
	}

	public FieldVisitor visitField(int access, String name, String desc,
			String signature, Object value) {
		if ((access & Opcodes.ACC_FINAL) != 0) {
			finalFields.add(name);
		}
		return super.visitField(access, name, desc, signature, value);
	}

	public MethodVisitor visitMethod(int access, String name, String desc,
			String signature, String[] exceptions) {
		if (!done && instrument() && !isGenerated(name) && instrument(access)) {
			// CountLocals locals = new CountLocals(access,
			// super.visitMethod(access,name,desc,signature,exceptions));
			// int nextLocal = 0; // locals.getMaxLocals()+1;
			// LocalVariablesSorter lvs = new LocalVariablesSorter(access, desc,
			// super.visitMethod(access, name, desc, signature, exceptions));

			return new AllocateInstrumentMethod(access, name, desc, signature,
					exceptions, super.visitMethod(access, name, desc,
							signature, exceptions), logPointerChange);
		} else
			return super.visitMethod(access, name, desc, signature, exceptions);
	}

	public void visitEnd() {
		if (!done && instrument()) {
			done = true;
			try {
				GeneratorAdapter mg;
				Label start;
				Label end;

				// Call the Agent.reportHeap function which will conditionally
				// report the heap statistics.
				// Note: We cannot get the Heap Statistics from JVMTI and we
				// can't perform a JNI call from
				// the object allocation event callback so the call back sets a
				// flag when we want the
				// heap states to be reported after a GC.
				// Also note: Even though a GC can be forced there is no reason
				// to expect that the heap would
				// be at minimal size due to the asynchronous behaviour of
				// finalizers
				mg = new GeneratorAdapter(Opcodes.ACC_PRIVATE
						| Opcodes.ACC_STATIC, new Method(
						LOG_INTERNAL_ALLOC_INC, VOID_SIGNATURE),
						VOID_SIGNATURE, new Type[] {}, this);

				start = mg.mark();
				mg.loadArgs();
				mg.invokeStatic(LOG_INTERNAL_TYPE, Method.getMethod(LOG_INTERNAL_CLASS
						.getMethod(LOG_ALLOC_INC)));
				end = mg.mark();
				mg.returnValue();

				mg.catchException(start, end, JAVA_LANG_THROWABLE_TYPE);
				mg.returnValue();

				mg.endMethod();

				mg = new GeneratorAdapter(Opcodes.ACC_PRIVATE
						| Opcodes.ACC_STATIC, new Method(
						LOG_INTERNAL_ALLOC_DEC, VOID_SIGNATURE),
						VOID_SIGNATURE, new Type[] {}, this);

				start = mg.mark();
				mg.loadArgs();
				mg.invokeStatic(LOG_INTERNAL_TYPE, Method.getMethod(LOG_INTERNAL_CLASS
						.getMethod(LOG_ALLOC_DEC)));
				end = mg.mark();
				mg.returnValue();

				mg.catchException(start, end, JAVA_LANG_THROWABLE_TYPE);
				mg.returnValue();

				mg.endMethod();

				mg = new GeneratorAdapter(Opcodes.ACC_PRIVATE
						| Opcodes.ACC_STATIC, new Method(
						LOG_INTERNAL_ALLOC_REPORT, OBJECT_SITE_SIGNATURE),
						OBJECT_SIGNATURE, new Type[] {}, this);

				start = mg.mark();
				mg.loadArgs();
				// mg.push((int)0);
				mg.invokeStatic(LOG_INTERNAL_TYPE, Method.getMethod(LOG_INTERNAL_CLASS.getMethod(
						LOG_ALLOC_REPORT, Object.class, int.class))); // 
				end = mg.mark();
				mg.returnValue();

				mg.catchException(start, end, JAVA_LANG_THROWABLE_TYPE);
				mg.returnValue();

				mg.endMethod();

				mg = new GeneratorAdapter(Opcodes.ACC_PRIVATE
						| Opcodes.ACC_STATIC, new Method(
						LOG_INTERNAL_ALLOC_DONE, VOID_SIGNATURE),
						VOID_SIGNATURE, new Type[] {}, this);

				start = mg.mark();
				mg.loadArgs();
				mg.invokeStatic(LOG_INTERNAL_TYPE, Method.getMethod(LOG_INTERNAL_CLASS
						.getMethod(LOG_ALLOC_DONE)));
				end = mg.mark();
				mg.returnValue();

				mg.catchException(start, end, JAVA_LANG_THROWABLE_TYPE);
				mg.returnValue();

				mg.endMethod();

				mg = new GeneratorAdapter(Opcodes.ACC_PRIVATE
						| Opcodes.ACC_STATIC, new Method(
						LOG_INTERNAL_POINTER_CHANGE, POINTER_CHANGE_SIGNATURE),
						POINTER_CHANGE_SIGNATURE, new Type[] {}, this);

				start = mg.mark();
				mg.loadArgs();
				mg.invokeStatic(LOG_INTERNAL_TYPE, Method.getMethod(LOG_INTERNAL_CLASS.getMethod(
						LOG_POINTER_CHANGE, Object.class, Object.class,
						Object.class)));
				end = mg.mark();
				mg.returnValue();

				mg.catchException(start, end, JAVA_LANG_THROWABLE_TYPE);
				mg.returnValue();

				mg.endMethod();

				mg = new GeneratorAdapter(Opcodes.ACC_PRIVATE
						| Opcodes.ACC_STATIC, new Method(
						LOG_INTERNAL_STATIC_POINTER_CHANGE,
						STATIC_POINTER_CHANGE_SIGNATURE),
						STATIC_POINTER_CHANGE_SIGNATURE, new Type[] {}, this);

				start = mg.mark();
				mg.loadArgs();
				mg.invokeStatic(LOG_INTERNAL_TYPE, Method.getMethod(LOG_INTERNAL_CLASS.getMethod(
						LOG_STATIC_POINTER_CHANGE, Object.class, Class.class,
						Object.class)));
				end = mg.mark();
				mg.returnValue();

				mg.catchException(start, end, JAVA_LANG_THROWABLE_TYPE);
				mg.returnValue();

				mg.endMethod();
			} catch (NoSuchMethodException nsme) {
				System.err.println("Unable to find " + LOG_INTERNAL);
				System.err.println("M:" + nsme);
				nsme.printStackTrace();
			}
		}

		super.visitEnd();
	}

	protected int getNextAllocateSiteId() {
		synchronized (this.state) {
			String valueStr = this.state.getProperty(AllocateInstrument.PROP_SITE_ID);
			int value = 1;
			if (valueStr!=null) value = Integer.parseInt(valueStr);
			this.state.setProperty(AllocateInstrument.PROP_SITE_ID, Integer.toString(value+1));
			return value;
		}
	}
	
	private boolean instrument() {
		if ((access & Opcodes.ACC_INTERFACE) != 0)
			return false;

		for (String k : excludePackages) {
			if (name.startsWith(k))
				return false;
		}

		return true;
	}

	private boolean instrument(int access) {
		return (access & Opcodes.ACC_ABSTRACT) == 0
				&& (access & Opcodes.ACC_BRIDGE) == 0
				&& (access & Opcodes.ACC_NATIVE) == 0;
	}

	private class AllocateInstrumentMethod extends AdviceAdapter {
		private static final String CONSTRUCTOR = "<init>";

		boolean constructor;
		boolean firstInstruction;

		private String encodedName;
		private int localsBase = 0;
		private int maxLocals;
		private MethodVisitor mv;
		private Label methodStartLabel;
		private String[] exceptions;
		private boolean methodDone;
		private boolean logPointerChanges;
		private boolean doneSuperConstructor;
		private TreeMap<String, String> delayedFieldPointer = new TreeMap<String, String>();

		private static final boolean DO_INC_DEC = true;
		private static final boolean DO_NEW_INVOKESPECIAL_SEQUENCE = true;

		private int siteNumber;
		
		// LinkedList<String> newTypeStack = new LinkedList<String>();
		LinkedList<Triplet> newTypeStack = new LinkedList<Triplet>();

		AllocateInstrumentMethod(int access, String methodName, String desc,
				String signature, String[] exceptions, MethodVisitor mv,
				boolean logPointerChanges) {
			super(mv, access, methodName, desc);
			this.mv = mv;
			this.constructor = DO_INC_DEC && CONSTRUCTOR.equals(methodName);
			this.firstInstruction = constructor;
			this.methodStartLabel = null;
			this.exceptions = exceptions;
			this.methodDone = false;
			this.doneSuperConstructor = !constructor;
			this.encodedName = encodeMethodName(name, methodName, desc);
			if (!methodToLargestLocal.containsKey(this.encodedName))
				methodToLargestLocal.put(this.encodedName, getArgumentSizes(access, desc));
			this.localsBase = methodToLargestLocal.get(this.encodedName);
			this.maxLocals = this.localsBase;
			this.siteNumber = 0;
		}

		public void onMethodExit(int opcode) {
			if (opcode != Opcodes.ATHROW)
				addDec();
		}

		public int getNextSiteId() {
			Integer siteId = getNextAllocateSiteId();
			if (allocateLogBaseName!=null) {
				try {
					Integer intraMethodSiteId = this.siteNumber++;
					Object[] siteData = {
						siteId,
						this.encodedName,
						intraMethodSiteId
					};
					write(allocateLogBaseName, siteData);
				} catch (FileNotFoundException fnfe) { }
			}
			return siteId;
		}
		
		public void visitFieldInsn(int opcode, String owner, String fieldName,
				String desc) {
			if (firstInstruction)
				addInc();
			if (logPointerChange && opcode == Opcodes.PUTFIELD
					&& desc.charAt(0) == 'L') {
				if (constructor && !doneSuperConstructor && name.equals(owner)
						&& finalFields.contains(fieldName))
					delayedFieldPointer.put(fieldName, desc);
				else {
					// instrument reference changes from
					// putfield ...,obj,v' => ...
					// to
					// dup2 ...,obj,v' => ...,obj,v',obj,v'
					// swap ...,obj,v',obj,v' => ...,obj,v',v',obj
					// dup ...,obj,v',v',obj => ...,obj,v',v',obj,obj
					// getfield ...,obj,v',v',obj,obj => ...,obj,v',v',obj,v
					// invokespecial
					// pointerchangelog(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V
					// ...,obj,v',v',obj,v => ...,obj,v'
					// putfield ...,obj,v' =>
					super.visitInsn(Opcodes.DUP2);
					super.visitInsn(Opcodes.SWAP);
					super.visitInsn(Opcodes.DUP);
					super.visitFieldInsn(Opcodes.GETFIELD, owner, fieldName,
							desc);
					super.visitMethodInsn(Opcodes.INVOKESTATIC, name,
							LOG_INTERNAL_POINTER_CHANGE,
							POINTER_CHANGE_SIGNATURE);
				}
			} else if (logPointerChange && opcode == Opcodes.PUTSTATIC
					&& desc.charAt(0) == 'L') {
				// if (finalFields.contains(fieldName)) {
				// // assume field is initially null
				// super.visitInsn(Opcodes.DUP);
				// } else {
				// instrument reference changes from
				// putstatic ...,v' => ...
				// to
				// dup ...,v' => ...,v',v'
				// ldc owner.class ...,v',v' => ...,v',v',k
				// getstatic ...,v',v',k => ...,v',v',k,v
				// invokespecial
				// staticpointerchangelog(Ljava/lang/Object;Ljava/lang/Class;Ljava/lang/Object;)V
				// ...,v',v',k,v => ...,v'
				super.visitInsn(Opcodes.DUP);
				super.visitLdcInsn(Type.getObjectType(owner));
				super.visitFieldInsn(Opcodes.GETSTATIC, owner, fieldName, desc);
				super.visitMethodInsn(Opcodes.INVOKESTATIC, name,
						LOG_INTERNAL_STATIC_POINTER_CHANGE,
						STATIC_POINTER_CHANGE_SIGNATURE);
				// }
			}
			super.visitFieldInsn(opcode, owner, fieldName, desc);
		}

		public void visitInsn(int opcode) {
			if (firstInstruction)
				addInc();
			super.visitInsn(opcode);
		}

		public void visitJumpInsn(int opcode, Label label) {
			if (firstInstruction)
				addInc();
			super.visitJumpInsn(opcode, label);
		}

		public void visitLdcInsn(Object cst) {
			if (firstInstruction)
				addInc();
			super.visitLdcInsn(cst);
		}

		public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
			if (firstInstruction)
				addInc();
			super.visitLookupSwitchInsn(dflt, keys, labels);
		}

		public void visitMethodInsn(int opcode, String owner,
				String methodName, String desc) {
			boolean reportReflectConstruction = opcode == Opcodes.INVOKEVIRTUAL
					&& NEW_INSTANCE.equals(methodName)
					&& desc.endsWith(";")
					&& (JAVA_LANG_CLASS.equals(owner) || JAVA_LANG_CONSTRUCTOR
							.equals(owner));
			if (firstInstruction)
				addInc();
			Label wrapFrom = null;
			if (reportReflectConstruction && !constructor) {
				wrapFrom = super.mark();
				addInc();
			}
			super.visitMethodInsn(opcode, owner, methodName, desc);
			if (opcode == Opcodes.INVOKESPECIAL
					&& CONSTRUCTOR.equals(methodName)) {
				if (DO_NEW_INVOKESPECIAL_SEQUENCE && !newTypeStack.isEmpty()) {
					Triplet p = newTypeStack.removeLast();
					if (!p.type.equals(owner)) {
						System.err.println("Excepted type: " + p.type
								+ " found: " + owner);
						System.exit(10);
					}
					super.visitVarInsn(Opcodes.ALOAD, p.var);
					addLog(false, p.siteId);
				}
				if (superName.equals(owner)) {
					doneSuperConstructor = true;
					// now log all the pointer changes for final fields
					for (String fieldName : delayedFieldPointer.keySet()) {
						// aload_0
						// getfield
						// aload_0
						// aconst_null
						// invoke
						super.visitVarInsn(Opcodes.ALOAD, 0);
						super.visitFieldInsn(Opcodes.GETFIELD, name, fieldName,
								delayedFieldPointer.get(fieldName));
						super.visitVarInsn(Opcodes.ALOAD, 0);
						super.visitInsn(Opcodes.ACONST_NULL);
						super.visitMethodInsn(Opcodes.INVOKESTATIC, name,
								LOG_INTERNAL_POINTER_CHANGE,
								POINTER_CHANGE_SIGNATURE);
					}
				}
			} else if (reportReflectConstruction) {
				int   siteId = getNextSiteId();
				if (!constructor) {
					Label wrapTo = super.mark();
					addInc();
					addLog(true, siteId);
					Label target = super.newLabel();
					super.visitJumpInsn(Opcodes.GOTO, target);
					super.catchException(wrapFrom, wrapTo, JAVA_LANG_THROWABLE_TYPE);
					addDec();
					super.visitMethodInsn(Opcodes.INVOKESTATIC, name,
							LOG_INTERNAL_ALLOC_DONE, VOID_SIGNATURE);
					super.visitInsn(Opcodes.ATHROW);
					super.mark(target);
				} else {
					addLog(true,siteId);
				}
			}
		}

		public void visitTableSwitchInsn(int min, int max, Label dflt,
				Label[] labels) {
			if (firstInstruction)
				addInc();
			super.visitTableSwitchInsn(min, max, dflt, labels);
		}

		public void visitVarInsn(int opcode, int var) {
			if (firstInstruction)
				addInc();
			super.visitVarInsn(opcode, var);
		}

		public void visitMultiANewArrayInsn(String desc, int dims) {
			if (firstInstruction)
				addInc();
			super.visitMultiANewArrayInsn(desc, dims);
			int   siteId = getNextSiteId();
			addLog(true, siteId);
		}

		public void visitTypeInsn(int opcode, String type) {
			if (firstInstruction)
				addInc();
			super.visitTypeInsn(opcode, type);
			// we deal with Opcodes.NEW through the constructors
			if (opcode == Opcodes.ANEWARRAY) {
				int   siteId = getNextSiteId();
				addLog(true, siteId);
			} else if (DO_NEW_INVOKESPECIAL_SEQUENCE && opcode == Opcodes.NEW) {
				super.visitInsn(Opcodes.DUP);
				Triplet p = new Triplet();
				p.type = type;
				p.var = this.localsBase + 1 + newTypeStack.size();
				p.siteId = getNextSiteId();
				if (this.maxLocals < p.var) {
					this.maxLocals = p.var;
					methodToLargestLocal.put(this.encodedName, new Integer(
							p.var));
				}
				super.setLocalType(p.var, JAVA_LANG_OBJECT_TYPE); // super.newLocal(OBJECT_TYPE);
				newTypeStack.addLast(p);
				super.visitVarInsn(Opcodes.ASTORE, p.var);
			}
		}

		public void visitIntInsn(int opcode, int operand) {
			if (firstInstruction)
				addInc();
			super.visitIntInsn(opcode, operand);
			if (opcode == Opcodes.NEWARRAY) {
				int   siteId = getNextSiteId();
				addLog(true, siteId);
			}
		}

		public void visitEnd() {
			if (!methodDone && methodStartLabel != null && constructor) {
				methodDone = true;
				Label methodEndLabel = super.mark();
				super.catchException(methodStartLabel, methodEndLabel, Type
						.getType(RuntimeException.class));
				super.visitMethodInsn(Opcodes.INVOKESTATIC, name,
						LOG_INTERNAL_ALLOC_DEC, VOID_SIGNATURE);
				super.visitInsn(Opcodes.ATHROW);
				if (exceptions != null) {
					for (String ex : exceptions) {
						super.catchException(methodStartLabel, methodEndLabel,
								Type.getObjectType(ex));
						super.visitMethodInsn(Opcodes.INVOKESTATIC, name,
								LOG_INTERNAL_ALLOC_DEC, VOID_SIGNATURE);
						super.visitInsn(Opcodes.ATHROW);
					}
				}
			}
			super.visitEnd();
		}

		private void addInc() {
			if (constructor) {
				super.visitMethodInsn(Opcodes.INVOKESTATIC, name,
						LOG_INTERNAL_ALLOC_INC, VOID_SIGNATURE);
				methodStartLabel = super.mark();
			}
			firstInstruction = false;
		}

		private void addDec() {
			if (constructor)
				super.visitMethodInsn(Opcodes.INVOKESTATIC, name,
						LOG_INTERNAL_ALLOC_DEC, VOID_SIGNATURE);
		}

		private void addLog(boolean dup, int site) {
			if (dup)
				super.visitInsn(Opcodes.DUP);
			super.visitLdcInsn(new Integer(site));
			super.visitMethodInsn(Opcodes.INVOKESTATIC, name,
					LOG_INTERNAL_ALLOC_REPORT, OBJECT_SITE_SIGNATURE);
			if (!constructor)
				super.visitMethodInsn(Opcodes.INVOKESTATIC, name,
						LOG_INTERNAL_ALLOC_DONE, VOID_SIGNATURE);
		}

		private void addWhereAmI() {
			// 0: new #2; //class java/lang/Exception
			// 3: dup
			// 4: invokespecial #3; //Method java/lang/Exception."<init>":()V
			// 7: invokevirtual #4; //Method
			// java/lang/Exception.printStackTrace:()V
			// 10: return
			// super.visitTypeInsn(Opcodes.NEW, type);
			String exClass = Type.getInternalName(Exception.class);
			super.visitTypeInsn(Opcodes.NEW, exClass);
			super.visitInsn(Opcodes.DUP);
			super.visitMethodInsn(Opcodes.INVOKESPECIAL, exClass, "<init>",
					"()V");
			super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, exClass,
					"printStackTrace", "()V");
		}
	}
}
