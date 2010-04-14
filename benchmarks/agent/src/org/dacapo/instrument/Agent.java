package org.dacapo.instrument;

public final class Agent {

	public  static boolean firstReportSinceForceGC = false;

	private static Runtime runtime = Runtime.getRuntime();
	
	static {
		localinit();
	}
	
	public static native void localinit();
	
	public static native boolean available();
	
	public static native void setLogFileName(String fileName);
	
	public static native void log(Thread thread, String event, String message);
	
	public static native void logAlloc(Thread thread, Object obj);
	
	public static native void start();
	
	public static native void stop();
	
	public static native void logMonitorEnter(Thread thread, Object obj);
	
	public static native void logMonitorExit(Thread thread, Object obj);

	public static void reportHeapAfterForceGC() {
		if (firstReportSinceForceGC) {
			reportHeapAfterForceGCSync();
		}
	}

	private static synchronized void reportHeapAfterForceGCSync() {
		if (firstReportSinceForceGC) {
			firstReportSinceForceGC = false;
			reportHeap();
		}
	}
	
	public static void reportHeap() {
		long free  = runtime.freeMemory();
		long max   = runtime.maxMemory();
		long total = runtime.totalMemory();
		
		long used  = total - free;

		writeHeapReport(Thread.currentThread(), used, free, total, max);
	}
	
	private static native void writeHeapReport(Thread thread, long used, long free, long total, long max);
}
