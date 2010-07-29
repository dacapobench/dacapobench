package org.dacapo.analysis.ckjm;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.TreeSet;

import gr.spinellis.ckjm.*;
import org.apache.bcel.generic.Type;
import org.dacapo.analysis.util.events.Event;
import org.dacapo.analysis.util.events.EventClassPrepare;
import org.dacapo.analysis.util.events.EventListener;
import org.dacapo.analysis.util.events.EventParseException;
import org.dacapo.analysis.util.events.EventStart;
import org.dacapo.analysis.util.events.EventStop;

import org.apache.bcel.Repository;
import org.apache.bcel.util.ClassPath;
import org.apache.bcel.util.SyntheticRepository;

import org.dacapo.instrument.LogTags;
import org.dacapo.util.CSVInputStream;
import org.dacapo.util.LogFiles;

public class CKJM {
	
	private static final String CLASS_EXTENSION          = ".class";
	
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
		
		LinkedList<File> fileList = LogFiles.orderedLogFileList(commandLine.getLogFile());
		
		LinkedList<String> classList = makeClassList(fileList,commandLine.getLogged());

		DaCapoOutputHandler output = new DaCapoOutputHandler(commandLine.getOutputStream(),commandLine.getIndividual());
		
		String[] classFileList = makeClassFileList(classList, commandLine.getClassPath());
		
		MetricsFilter.runMetrics(classFileList, output);
		output.reportTotal();
	}
	
	private static class Listener extends EventListener {
		LinkedList<String> classList = new LinkedList<String>();
		
		public void processStart() { }
		
		public void processEvent(Event e) {
			if (getLogOn() && e.getLogPrefix()==EventClassPrepare.TAG) {
				String className = ((EventClassPrepare)e).getClassName(); 
				classList.add(className.substring(1,className.length()-1).replace('/','.'));
			}
		}
		
		public void processStop() { }
	};
	
	private static LinkedList<String> makeClassList(LinkedList<File> fileList, boolean loggedOnly) throws IOException, CSVInputStream.CSVException, EventParseException {
		Listener listener = new Listener();
		
		for(File f: fileList) {
			CSVInputStream cis = new CSVInputStream(new FileInputStream(f));

			Event.parseEvents(listener, cis);
		}
		
		return listener.classList;
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
			index++;
		}
		
		return fileList;
	}
	
	private static File findClassFile(String klass, LinkedList<String> pathList) {
		for(String path: pathList) {
			File file = new File(new File(path),klass.replace('.', '/')+CLASS_EXTENSION);
			if (file.exists() && file.canRead()) return file;
		}
		
		return null;
	}
}