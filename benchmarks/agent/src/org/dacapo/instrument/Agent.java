package org.dacapo.instrument;

public final class Agent {

	public  static boolean firstReportSinceForceGC = false;

	private static Runtime runtime = Runtime.getRuntime();
	private static long    callChainCount     = 0;
	private static long    callChainFrequency = 1;
	private static long    agentIntervalTime  = 0; // the number of milliseconds the agent should act on
	private static boolean callChainEnable = false;
	private static Object  waiter = new Object();
	private static AgentThread agentThread = null;
	
	private static final String  LOG_PREFIX_CLASS_INITIALIZATION = "CI"; 
	
	private static class AgentThread extends Thread {
		boolean agentThreadStarted = false;
		boolean logon = false;
		
		public AgentThread() {
			super("agent thread");
			
			setDaemon(true);
		}

		public synchronized boolean setLogon(boolean value) {
			boolean tmp = logon;
			logon = value;
			if (!tmp && value) notify();
			return value != tmp;
		}
		
		private synchronized void startup() {
			agentThreadStarted = true;
			notify();
		}
		
		public synchronized void started() {
			while (! agentThreadStarted) {
				try { wait(); } catch (Exception e) { }
			}
		}
		
		public void run() {
			startup();

			if (0 < agentIntervalTime)
				for(;;) intervalAgent();
		}
		
		private synchronized void waitForLogon() {
			while (! logon) {
				try { wait(); } catch (Exception e) { }
			}
		}
		
		private void intervalAgent() {
			// set a start time.
			long currentTime = System.currentTimeMillis();
			
			waitForLogon();
			
			agentThread(this);
			
			// set up an alarm...
			long tempTime  = System.currentTimeMillis();
			long sleepTime = agentIntervalTime + tempTime - currentTime;
			
			// set new current time, to next interval
			currentTime += agentIntervalTime*((tempTime - currentTime)%agentIntervalTime + 1);
			
			// wait what ever time required
			if (sleepTime>0) 
				try { 
					this.sleep(sleepTime); 
				} catch (Exception e) { }
		}
	};
	
	static {
		localinit();
		
		agentThread = new AgentThread();
		
		agentThread.start();

		agentThread.started();
	}
	
	public static void localinit() { internalLocalInit(); }

	public static boolean available() { return internalAvailable(); }
	
	public static void setLogFileName(String fileName) { internalSetLogFileName(fileName); }
	
	public static void log(Thread thread, String event, String message) {
		internalLog(thread,event,message);
	}

	public static void logAlloc(Thread thread, Object obj) {
		new Exception().printStackTrace();
		// internalLogAlloc(thread,obj);
		System.exit(10);
	}
	
	public static void logCallChain(Thread thread) {
		// internalLogCallChain(thread);
	}
	
	public static void start() {
		if (agentThread.setLogon(true))
			internalStart();
	}
	
	public static void stop() {
		if (agentThread.setLogon(false))
			internalStop();
	}
	
	public static void logMonitorEnter(Thread thread, Object obj) { internalLogMonitorEnter(thread, obj); }
	
	public static void logMonitorExit(Thread thread, Object obj) { internalLogMonitorExit(thread, obj); }
	
	public static void logCallChain() {
		if (callChainEnable && ((++callChainCount)%callChainFrequency) == 0) {
			logCallChain(Thread.currentThread());
		}
	}

	public static void reportHeapAfterForceGC() {
		if (firstReportSinceForceGC) {
			reportHeapAfterForceGCSync();
		}
	}

	public static void reportHeap() {
		long free  = runtime.freeMemory();
		long max   = runtime.maxMemory();
		long total = runtime.totalMemory();
		
		long used  = total - free;

		internalHeapReport(Thread.currentThread(), used, free, total, max);
	}
	
	public static void reportClass(String className) {
		internalLog(Thread.currentThread(),LOG_PREFIX_CLASS_INITIALIZATION,className);
	}
	
	protected static native void agentThread(Thread thread);

	private static native void internalLocalInit();

	private static native void internalLog(Thread thread, String event, String message);
	
	private static native void internalLogAlloc(Thread thread, Object obj);
	
	private static native void internalLogMonitorEnter(Thread thread, Object obj);
	
	private static native void internalLogMonitorExit(Thread thread, Object obj);
	
	// private static native void internalLogCallChain(Thread thread);
	
	private static native boolean internalAvailable();
	
	private static native void internalSetLogFileName(String fileName);
	
	private static native void internalStart();
	
	private static native void internalStop();

	private static synchronized void reportHeapAfterForceGCSync() {
		if (firstReportSinceForceGC) {
			reportHeap();
		}
	}
	
	private static native void internalHeapReport(Thread thread, long used, long free, long total, long max);

}
