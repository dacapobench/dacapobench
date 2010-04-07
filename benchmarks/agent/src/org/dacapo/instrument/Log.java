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
		new Exception().printStackTrace();
		System.exit(1);
	}

	public static void reportClass(String className) {
		new Exception().printStackTrace();
		System.exit(1);
	}
	
	public static boolean reportMethod(String className, String methodName, String signature) {
		new Exception().printStackTrace();
		System.exit(1);
		return true;
	}
	
	public static void reportMonitorEnter(Object obj) {
		if (enableLogging) {
			if (available)
				Agent.logMonitorEnter(Thread.currentThread(),obj);
			else
				out.println("ME:"+Thread.currentThread()+":"+obj.hashCode());
		}
	}
	
	public static void reportMonitorExit(Object obj) {
		if (enableLogging) {
			if (available)
				Agent.logMonitorExit(Thread.currentThread(),obj);
			else
				out.println("MX:"+Thread.currentThread()+":"+obj.hashCode());
		}
	}
	
	public static void reportBlank() {
		if (enableLogging) {
			if (available)
				Agent.log(Thread.currentThread(),"_","blank");
			else
				out.println("blank");
		}
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
