package org.dacapo.instrument;

public final class Agent {

	public static boolean firstReportSinceForceGC = false;

	private static Runtime runtime = Runtime.getRuntime();
	private static long callChainCount = 0;
	private static long callChainFrequency = 1;
	private static long agentIntervalTime = 0; // the number of milliseconds the
												// agent should act on
	private static boolean callChainEnable = false;
	private static AgentThread agentThread = null;

	private static ThreadLocal<DelayAllocReport> delayAllocReport = new ThreadLocal<DelayAllocReport>();

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
			if (!tmp && value)
				notify();
			return value != tmp;
		}

		private synchronized void startup() {
			agentThreadStarted = true;
			notify();
		}

		public synchronized void started() {
			while (!agentThreadStarted) {
				try {
					wait();
				} catch (Exception e) {
				}
			}
		}

		public void run() {
			startup();

			if (0 < agentIntervalTime)
				for (;;)
					intervalAgent();
		}

		private synchronized void waitForLogon() {
			while (!logon) {
				try {
					wait();
				} catch (Exception e) {
				}
			}
		}

		private void intervalAgent() {
			// set a start time.
			long currentTime = System.currentTimeMillis();

			waitForLogon();

			agentThread(this);

			// set up an alarm...
			long tempTime = System.currentTimeMillis();
			long sleepTime = agentIntervalTime + tempTime - currentTime;

			// set new current time, to next interval
			currentTime += agentIntervalTime
					* ((tempTime - currentTime) % agentIntervalTime + 1);

			// wait what ever time required
			if (sleepTime > 0)
				try {
					Thread.sleep(sleepTime);
				} catch (Exception e) {
				}
		}
	};

	private static class DelayAllocReport {
		private static final int DEFAULT_SIZE = 16;

		private static final int BEFORE = 0;
		private static final int OBJECT = 1;
		private static final int AFTER = 2;
		private static final int POINTERS_PER_PUTFIELD = 3;

		int delayCount = 0;

		Object[][] nodes = new Object[DEFAULT_SIZE][DEFAULT_SIZE];
		int[][]    sites = new int[DEFAULT_SIZE][DEFAULT_SIZE];
		int[] depth = new int[DEFAULT_SIZE];
		int size = 0;

		Object[] putfields = new Object[POINTERS_PER_PUTFIELD * DEFAULT_SIZE];
		int putfieldsSize = 0;

		void extend(int newSize) {
			if (nodes.length <= newSize) {
				// System.err.println("Extending#"+hashCode()+":to "+newSize);
				newSize = DEFAULT_SIZE * ((newSize / DEFAULT_SIZE) + 1);

				Object[][] tmpNodes = new Object[newSize][];
				int[][]    tmpSites = new int[newSize][];
				int[] tmpDepth = new int[newSize];

				for (int i = 0; i < nodes.length; ++i) {
					tmpNodes[i] = nodes[i];
					tmpSites[i] = sites[i];
					tmpDepth[i] = depth[i];

					nodes[i] = null;
					sites[i] = null;
				}
				for (int i = nodes.length; i < newSize; ++i) {
					tmpNodes[i] = new Object[DEFAULT_SIZE];
					tmpSites[i] = new int[DEFAULT_SIZE];
					tmpDepth[i] = 0;
				}

				nodes = tmpNodes;
				sites = tmpSites;
				depth = tmpDepth;
			}
			size = newSize;
		}

		void inc() {
			++delayCount;
			// System.err.println("INC#"+hashCode()+":"+delayCount);
		}

		void dec() {
			--delayCount;
			// System.err.println("DEC#"+hashCode()+":"+delayCount);
		}

		void report(Object obj) {
			report(obj,0);
		}
		
		void report(Object obj,int site) {
			if (obj == null)
				return;
			if (size <= delayCount)
				extend(delayCount + 1);
			Object[] nodeStack = nodes[delayCount];
			int[]    siteStack = sites[delayCount];
			int slot = depth[delayCount];
			if (nodeStack.length <= slot) {
				Object[] tmpNodeStack = new Object[nodeStack.length + DEFAULT_SIZE];
				int[]    tmpSiteStack = new int[nodeStack.length + DEFAULT_SIZE];
				// System.err.println("AddSlot#"+hashCode()+":["+delayCount+"]to "+stack.length);
				for (int i = 0; i < nodeStack.length; ++i) {
					tmpNodeStack[i] = nodeStack[i];
					tmpSiteStack[i] = siteStack[i];
					nodeStack[i] = null;
					siteStack[i] = 0;
				}
				nodeStack = tmpNodeStack;
				siteStack = tmpSiteStack;
				nodes[delayCount] = nodeStack;
			}
			nodeStack[slot] = obj;
			siteStack[slot] = site;
			depth[delayCount] = slot + 1;
		}

		void putfield(Object after, Object obj, Object before) {
			if (0 < delayCount) {
				if (putfields.length < putfieldsSize) {
					Object[] tmp = new Object[putfields.length
							+ POINTERS_PER_PUTFIELD * DEFAULT_SIZE];
					for (int i = 0; i < putfields.length; i++)
						tmp[i] = putfields[i];
					putfields = tmp;
				}
				putfields[putfieldsSize + BEFORE] = before;
				putfields[putfieldsSize + OBJECT] = obj;
				putfields[putfieldsSize + AFTER] = after;
				putfieldsSize += POINTERS_PER_PUTFIELD;
			} else {
				internalLogPointerChange(Thread.currentThread(), after, obj,
						before);
			}
		}

		void done() {
			if (delayCount == 0) {
				Thread t = Thread.currentThread();
				for (int i = 0; i < size; ++i) {
					for (int j = 0, d = depth[i]; j < d; ++j) {
						if (nodes[i][j] != null) {
							// System.err.println("Alloc:"+nodes[i][j].getClass());
							internalAllocReport(t, nodes[i][j], nodes[i][j]
									.getClass(), sites[i][j]);
							if (firstReportSinceForceGC) {
								reportHeapAfterForceGC();
							}
							nodes[i][j] = null;
							sites[i][j] = 0;
						}
					}
					depth[i] = 0;
				}
				size = 0;
				for (int i = 0; i < putfieldsSize; i += 3) {
					internalLogPointerChange(t, putfields[i + AFTER],
							putfields[i + OBJECT], putfields[i + BEFORE]);
					putfields[i + BEFORE] = null;
					putfields[i + OBJECT] = null;
					putfields[i + AFTER] = null;
				}
				putfieldsSize = 0;
			}
		}
	};

	// private static class DelayAllocReport {
	// private static final int DEFAULT_SIZE = 16;
	//		
	// int delayCount = 0;
	//		
	// Object[][] nodes = new Object[DEFAULT_SIZE][DEFAULT_SIZE];
	// int[] depth = new int[DEFAULT_SIZE];
	// int size = 0;
	//		
	// void extend(int newSize) {
	// if (nodes.length <= newSize) {
	// newSize = DEFAULT_SIZE * ((newSize / DEFAULT_SIZE) + 1);
	//				
	// Object[][] tmpNodes = new Object[newSize][];
	// int[] tmpDepth = new int[newSize];
	//				
	// for(int i=0; i<nodes.length; ++i) {
	// tmpNodes[i] = nodes[i];
	// tmpDepth[i] = depth[i];
	//					
	// nodes[i] = null;
	// }
	// for(int i=nodes.length; i<newSize; ++i) {
	// tmpNodes[i] = new Object[DEFAULT_SIZE];
	// tmpDepth[i] = 0;
	// }
	//				
	// nodes = tmpNodes;
	// depth = tmpDepth;
	// }
	// size = newSize;
	// }
	// void inc() { ++delayCount; }
	// void dec() { --delayCount; }
	// void report(Object obj) {
	// if (size<=delayCount)
	// extend(delayCount+1);
	// Object[] stack = nodes[delayCount];
	// int slot = depth[delayCount];
	// if (stack.length <= slot) {
	// Object[] tmpStack = new Object[stack.length + DEFAULT_SIZE];
	// for(int i = 0; i < stack.length; ++i) {
	// tmpStack[i] = stack[i];
	// stack[i] = null;
	// }
	// stack = tmpStack;
	// nodes[delayCount] = stack;
	// }
	// stack[slot] = obj;
	// depth[delayCount] = slot + 1;
	// }
	// void done() {
	// for(int i = 0; i < size; ++i) {
	// for(int j = 0, d = depth[i]; j < d; ++j) {
	// if (nodes[i][j] != null) {
	// // internalAllocReport(nodes[i][j]);
	// nodes[i][j] = null;
	// }
	// }
	// depth[i] = 0;
	// }
	// }
	// };

	static {
		localinit();

		agentThread = new AgentThread();

		agentThread.start();

		agentThread.started();
	}

	public static void localinit() {
		internalLocalInit();
	}

	public static boolean available() {
		return internalAvailable();
	}
	
	public static void setLogFileName(String fileName) {
		internalSetLogFileName(fileName);
	}

	public static void log(Thread thread, String event, String message) {
		internalLog(thread, event, message);
	}

	public static boolean logMethod(String className, String methodName, String signature) {
		new Exception().printStackTrace();
		System.exit(1);
		return true;
	}

	public static void logThread() {
		internalLogThread(Thread.currentThread());
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
		if (agentThread.setLogon(true)) {
			internalStart();
		}
	}

	public static void stop() {
		if (agentThread.setLogon(false)) {
			internalStop();
		}
	}

	public static void logMonitorEnter(Object obj) {
		internalLogMonitorEnter(Thread.currentThread(), obj);
	}

	public static void logMonitorExit(Object obj) {
		internalLogMonitorExit(Thread.currentThread(), obj);
	}

	public static void logMonitorNotify(Object obj) {
		internalLogMonitorNotify(Thread.currentThread(), obj);
	}

	public static void logCallChain() {
		if (callChainEnable && ((++callChainCount) % callChainFrequency) == 0) {
			logCallChain(Thread.currentThread());
		}
	}

	public static void logPointerChange(Object after, Object obj, Object before) {
		if (delayAllocReport.get() == null)
			delayAllocReport.set(new DelayAllocReport());
		delayAllocReport.get().putfield(after, obj, before);
	}

	@SuppressWarnings("unchecked")
	public static void logStaticPointerChange(Object after, Class klass,
			Object before) {
		if (delayAllocReport.get() == null)
			delayAllocReport.set(new DelayAllocReport());
		internalLogStaticPointerChange(Thread.currentThread(), after, klass,
				before);
	}

	public static void allocInc() {
		if (delayAllocReport.get() == null)
			delayAllocReport.set(new DelayAllocReport());
		delayAllocReport.get().inc();
	}

	public static void allocDec() {
		delayAllocReport.get().dec();
	}

//	public static void allocReport(Object obj) {
//		allocReport(obj, 0);
//	}

	public static void allocReport(Object obj, int site) {
		if (delayAllocReport.get() == null)
			delayAllocReport.set(new DelayAllocReport());
		delayAllocReport.get().report(obj, site);
	}

	public static void allocDone() {
		if (delayAllocReport.get() != null)
			delayAllocReport.get().done();
	}

	public static void reportHeapAfterForceGC() {
		if (firstReportSinceForceGC) {
			firstReportSinceForceGC = false;
			reportHeapAfterForceGCSync();
		}
	}

	public synchronized static void reportHeap() {
		long free = runtime.freeMemory();
		long max = runtime.maxMemory();
		long total = runtime.totalMemory();

		long used = total - free;

		internalHeapReport(Thread.currentThread(), used, free, total, max);
	}

	public static void reportClass(String className) {
		internalLog(Thread.currentThread(),
				LogTags.LOG_PREFIX_CLASS_INITIALIZATION, className);
	}

	protected static native void agentThread(Thread thread);

	private static native void internalLocalInit();

	private static native void internalLog(Thread thread, String event,
			String message);

	private static native void internalLogThread(Thread thread);

	// private static native void internalLogAlloc(Thread thread, Object obj);

	private static native void internalLogPointerChange(Thread thread,
			Object after, Object obj, Object before);

	private static native void internalLogStaticPointerChange(Thread thread,
			Object after, Object obj, Object before);

	private static native void internalLogMonitorEnter(Thread thread, Object obj);

	private static native void internalLogMonitorExit(Thread thread, Object obj);

	private static native void internalLogMonitorNotify(Thread thread,
			Object obj);

	// private static native void internalLogCallChain(Thread thread);

	private static native boolean internalAvailable();

	private static native void internalSetLogFileName(String fileName);

	private static native void internalStart();

	private static native void internalStop();

	@SuppressWarnings("unchecked")
	private static native void internalAllocReport(Thread thread, Object obj, Class klass, int site);

	private static synchronized void reportHeapAfterForceGCSync() {
		if (firstReportSinceForceGC) {
			firstReportSinceForceGC = false;
			reportHeap();
		}
	}

	private static native void internalHeapReport(Thread thread, long used,
			long free, long total, long max);

}
