package org.dacapo.analysis.ckjm;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.LinkedList;

import gr.spinellis.ckjm.*;
import org.apache.bcel.generic.Type;
import org.dacapo.analysis.util.CSVInputStream;

import org.apache.bcel.Repository;
import org.apache.bcel.util.ClassPath;
import org.apache.bcel.util.SyntheticRepository;

public class CKJM {
	
	private static final String DEFAULT_EXTENSION        = ".csv";
	private static final String CLASS_EXTENSION          = ".class";
	
	private static final String LOG_PREFIX_START         = "START";
	private static final String LOG_PREFIX_STOP          = "STOP";
	private static final String LOG_PREFIX_CLASS_PREPARE = "LD";
	
	public static void main(String[] args) throws Exception {
		CommandLineArgs commandLine = new CommandLineArgs(args);
		
		StringBuffer classPathString = new StringBuffer();
		for(String c: commandLine.getClassPath()) {
			if (classPathString.length()!=0)
				classPathString.append(":"+c);
			else
				classPathString.append(c);
		}
		ClassPath.setDefaultClassPath(new ClassPath(classPathString.toString()));
		
		LinkedList<File> fileList = makeFileList(commandLine.getLogFile());
		
		LinkedList<String> classList = makeClassList(fileList,commandLine.getLogged());

		DaCapoOutputHandler output = new DaCapoOutputHandler(commandLine.getOutputStream(),commandLine.getIndividual());
		
		String[] classFileList = makeClassFileList(classList, commandLine.getClassPath());
		
//		System.err.println("Load Classes into repository");
//		for(String className: classList) {
//			try {
//				System.err.println("  Class:"+className);
//				syntheticRepository.loadClass(className);
//			} catch (ClassNotFoundException cnfe) {
//				System.err.println("Unable to load "+className);
//			}
//		}
//		System.err.println("Process metrics");
		
		MetricsFilter.runMetrics(classFileList, output);
		output.reportTotal();
	}
	
	private static LinkedList<File> makeFileList(String fileName) {
		LinkedList<File> list           = new LinkedList<File>();
		int              extensionPoint = fileName.lastIndexOf('.');

		String extension = DEFAULT_EXTENSION;
		String baseName  = fileName;
		
		if (0 < extensionPoint) {
			extension = fileName.substring(extensionPoint);
			baseName  = fileName.substring(0,extensionPoint);
		}
		
		int fileNumber = 0;
		File file = null;
		do {
			file = new File(baseName+"-"+fileNumber+extension);
			if (file.exists() && file.canRead())
				list.add(file);
			else
				file = null;
			fileNumber++;
		} while (file != null);
		
		return list;
	}
	
	private static LinkedList<String> makeClassList(LinkedList<File> fileList, boolean loggedOnly) throws IOException, CSVInputStream.CSVException {
		LinkedList<String> classList = new LinkedList<String>();
		
		for(File f: fileList) {
			CSVInputStream cis = new CSVInputStream(new FileInputStream(f));
			
			boolean logPeriod = false;
			
			while (cis.nextRow()) {
				String logTag = cis.nextFieldString();
				
				if (logTag.equals(LOG_PREFIX_START))
					logPeriod = true;
				else if (logTag.equals(LOG_PREFIX_STOP))
					logPeriod = false;
				else if ((!loggedOnly || logPeriod) && logTag.equals(LOG_PREFIX_CLASS_PREPARE)) {
					long   tag       = cis.nextFieldLong();
					String className = cis.nextFieldString();
					
					classList.add(className.substring(1,className.length()-1).replace('/','.'));
				}
			}
		}
		
		return classList;
	}
	
	private static String[] makeClassFileList(LinkedList<String> classList, LinkedList<String> pathList) {
		LinkedList<File> classFileList = new LinkedList<File>();
		
		for(String klass: classList) {
			File file = findClassFile(klass,pathList);
			
			if (file != null) classFileList.add(file);
		}
		
		String[] fileList = new String[classFileList.size()];
		
		int index = 0;
		for(File f: classFileList) {
			fileList[index] = f.toString(); 
//			fileList[index] = "/tmp/store.jar "+f.toString();
			index++;
		}
		
		return fileList;
	}
	
	private static File findClassFile(String klass, LinkedList<String> pathList) {
		for(String path: pathList) {
			File file = new File(new File(path),klass.replace('.', '/')+CLASS_EXTENSION);
//			File file = new File(new File(path),klass+CLASS_EXTENSION);
			
//			if (file.exists() && file.canRead()) return new File(klass.replace('.','/')+CLASS_EXTENSION);
			if (file.exists() && file.canRead()) return file;
		}
		
		return null;
	}
}