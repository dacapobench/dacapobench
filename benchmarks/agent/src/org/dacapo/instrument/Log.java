package org.dacapo.instrument;

import java.io.File;
import java.io.PrintStream;

public final class Log {

	private static PrintStream out = null;
	
	private static final String LOGFILE_ENV = "DACAPO_LOGFILE";
	private static final String DEFAULT_LOG_FILE_NAME = "dacapo.log";
	
	private static boolean available;
	private static boolean enableLogging = false;
	
	static {
		String logFileName = System.getenv(LOGFILE_ENV);
		
		if (logFileName == null) {
			logFileName = DEFAULT_LOG_FILE_NAME;
		}
			
		try {
			available = Agent.available();
		} catch (UnsatisfiedLinkError e) {
			available = false;
		}
		
		if (! available) {
			try {
				out = new PrintStream(new File(logFileName));
			} catch (Exception nlf) {
			
			if (out == null)
				out = System.err;
			}
		}
	}
	
	public static void reportAlloc(Object obj) {
		obj.hashCode();
		String m = obj.hashCode()+":"+obj.getClass().getName();
		if (available && enableLogging)
			Agent.log(Thread.currentThread(),"A",m);
		else
			out.println(m);
	}

	public static void reportClass(String className) {
		String m = className;
		if (available && enableLogging)
			Agent.log(Thread.currentThread(),"C",m);
		else
			out.println(m);
	}
	
	public static boolean reportMethod(String className, String methodName, String signature) {
		String m = className+"."+methodName+signature;
		if (available && enableLogging)
			Agent.log(Thread.currentThread(),"M",m);
		else
			out.println(m);
		return true;
	}
	
	public static void reportMonitorEnter(Object obj) {
		if (available && enableLogging)
			Agent.logMonitorEnter(Thread.currentThread(),obj);
		else
			out.println("ME:"+Thread.currentThread()+":"+obj.hashCode());
	}
	
	public static void reportMonitorExit(Object obj) {
		if (available && enableLogging)
			Agent.logMonitorExit(Thread.currentThread(),obj);
		else
			out.println("MX:"+Thread.currentThread()+":"+obj.hashCode());
	}
	
	public static void reportBlank() {
		if (available && enableLogging)
			Agent.log(Thread.currentThread(),"_","blank");
		else
			out.println("blank");
	}
	
	public static void main(String[] args) {
		start();
		stop();
	}
	
	public static synchronized void start() {
		if (! enableLogging) {
			enableLogging = true;
			if (available) Agent.start();
		}
	}
	
	public static synchronized void stop() {
		if (enableLogging) {
			if (available) Agent.stop();
			enableLogging = false;
		}
	}
	
}
