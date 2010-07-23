package org.dacapo.instrument.instrumenters;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Properties;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import org.objectweb.asm.ClassVisitor;

public final class InstrumenterFactory {

	public static String FRAMEWORK = "yo";
	
	private static final String DEPENDENCIES_FIELD = "DEPENDENCIES";
	private static final String MAKE_TRANSFORM = "make";
	
	private static LinkedList<Method> orderedTransforms = new LinkedList<Method>();
	
	static {
		initFramework();
	}
	
	public static ClassVisitor makeTransformChain(ClassVisitor cv, TreeMap<String,Integer> methodToLargestLocal, Properties options, Properties state) {
		try {
			for(Method functor: orderedTransforms) {
				cv = (ClassVisitor)(functor.invoke(null, cv, methodToLargestLocal, options, state));
			}
			return cv;
		} catch (Throwable t) {
			t.printStackTrace();
			return null;
		}
	}
	
	private static void initFramework() {
		TreeSet<String> classNameSet = getInstrumenters();
		TreeMap<String, Class> nameToClass = new TreeMap<String, Class>(); 

		TreeMap<String, TreeSet<String>> graph = new TreeMap<String, TreeSet<String>>();
		TreeMap<String, Method> functors = new TreeMap<String, Method>();
		
		LinkedList<String> instrumenters = new LinkedList<String>();
		
		if (classNameSet != null) {
			for(String klassName: classNameSet) {
				try {
					Class   klass  = Instrumenter.class.getClassLoader().loadClass(klassName);
					Method  method = klass.getDeclaredMethod(MAKE_TRANSFORM, ClassVisitor.class, TreeMap.class, Properties.class, Properties.class);
					Field   field  = klass.getDeclaredField(DEPENDENCIES_FIELD);

					functors.put(klassName, method);
					Class[] fromList  = (Class[])(klass.getField(DEPENDENCIES_FIELD).get(null));
					if (! graph.containsKey(klassName)) {
						graph.put(klassName, new TreeSet<String>());
					}
					for(Class from: fromList) {
						graph.get(klassName).add(from.getName());
					}
				} catch (Throwable t) {
					// this is a non-compliant so we do not include it in the transform chain
				}
			}
		}

		while (! graph.isEmpty()) {
			LinkedList<String> noDependencies = new LinkedList<String>();
			
			for(String klass: graph.keySet()) {
				if (graph.get(klass).isEmpty()) {
					noDependencies.add(klass);
				}
			}
			
			for(String klass: noDependencies) {
				graph.remove(klass);
			}
			
			if (noDependencies.isEmpty()) {
				System.err.println("Circular dependencies on transform sets:");
				for(String klass: graph.keySet()) {
					boolean first = true;
					System.err.print(klass+" <- ");
					for(String from: graph.get(klass)) {
						if (first) first = false;
						else {
							System.err.print(", ");
						}
						System.err.print(from);
						System.err.println();
					}
				}
				System.exit(10);
			}
			
			for(String klass: noDependencies) {
				for(String target: graph.keySet())
					graph.get(target).remove(klass);
			}

			instrumenters.addAll(noDependencies);
		}
		
		for(String klass: instrumenters) {
			orderedTransforms.addLast(functors.get(klass));
		}
	}

	private static TreeSet<String> getInstrumenters() {
		ClassLoader cl = Instrumenter.INSTRUMENTER_CLASS.getClassLoader();
		
		try {
			URL u = cl.getResource(Instrumenter.INSTRUMENTER_PACKAGE);
			if (u != null) {
				if ("file".equals(u.getProtocol())) {
					return getSetFromFileSystem(new File(u.getPath()));
				} else if ("jar".equals(u.getProtocol())) {
					return getSetFromJar(new URL(u.getPath()));
				}
			}
		} catch (Throwable t) { }
		return null;
	}
	
	private static TreeSet<String>  getSetFromFileSystem(File dir) {
		TreeSet<String> instrumenters = new TreeSet<String>();
		
		if (dir.isDirectory()) {
			for(File f: dir.listFiles()) {
				String name = f.getName();
				if (f.isFile() && 
					name.indexOf('$')==-1 && 
					name.toLowerCase().endsWith(Instrumenter.CLASS_SUFFIX)) {
					name = (Instrumenter.INSTRUMENTER_PACKAGE + "/" + name.substring(0,name.length()-Instrumenter.CLASS_SUFFIX.length())).replace('/','.');
					instrumenters.add(name);
				}
			}
		}
		
		return instrumenters;
	}
	
	private static TreeSet<String> getSetFromJar(URL url) {
		TreeSet<String> instrumenters = new TreeSet<String>();
		
		try {
			if (url.toExternalForm().indexOf('!')!=-1) {
				url = new URL(url.toExternalForm().substring(0,url.toExternalForm().indexOf('!')));
			}

			InputStream is = url.openStream();
			
			if (is != null) {
				try {
					JarInputStream jis = new JarInputStream(is);
					
					JarEntry entry;
		
					while ((entry = jis.getNextJarEntry()) != null) {
						String name = entry.getName();
						if (!entry.isDirectory() &&
							name.startsWith(Instrumenter.INSTRUMENTER_PACKAGE) &&
							name.indexOf('$')==-1 &&
							name.endsWith(Instrumenter.CLASS_SUFFIX)) {
							name = name.substring(0,name.length()-Instrumenter.CLASS_SUFFIX.length()).replace('/', '.');
							instrumenters.add(name);
						}
					}
				} finally {
					is.close();
				}
			}
		} catch (Throwable t) {
			
		}
		
		return instrumenters;
	}
}
