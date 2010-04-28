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
	
	public static native void localinit();
	
	public static native boolean available();
	
	public static native void setLogFileName(String fileName);
	
	public static native void log(Thread thread, String event, String message);
	
	public static native void logAlloc(Thread thread, Object obj);
	
	public static void start() {
		if (agentThread.setLogon(true))
			internalStart();
	}
	
	private static native void internalStart();
	
	public static void stop() {
		if (agentThread.setLogon(false))
			internalStop();
	}
	
	private static native void internalStop();
	
	public static native void logMonitorEnter(Thread thread, Object obj);
	
	public static native void logMonitorExit(Thread thread, Object obj);
	
	public static native void logCallChain(Thread thread);
	
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

	protected static native void agentThread(Thread thread);
}
