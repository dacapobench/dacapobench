package org.dacapo.instrument;

final class Agent {

	public static native boolean available();
	
	public static native void setLogFileName(String fileName);
	
	public static native void log(Thread thread, String event, String message);
	
	public static native void logAlloc(Thread thread, Object obj);
	
	public static native void start();
	
	public static native void stop();
	
	public static native void logMonitorEnter(Thread thread, Object obj);
	
	public static native void logMonitorExit(Thread thread, Object obj);
}
