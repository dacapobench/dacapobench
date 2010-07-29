package org.dacapo.analysis.thread;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.LinkedList;
import java.util.TreeMap;
import java.util.TreeSet;

import org.dacapo.analysis.util.events.*;
import org.dacapo.analysis.vm.VM;
import org.dacapo.analysis.vm.VMClass;
import org.dacapo.analysis.vm.VMClasses;
import org.dacapo.analysis.vm.VMThreadState;
import org.dacapo.analysis.vm.VMThreadStates;

import org.dacapo.instrument.LogTags;
import org.dacapo.util.*;

public class Concurrency {

	
	private final static String EOL = System.getProperty("line.separator");
	
	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		CommandLineArgs options = new CommandLineArgs(args);
		
		CSVOutputStream lockLog = null;
		
		if (options.getLockLogFile()!=null) 
			lockLog = new CSVOutputStream(new FileOutputStream(new File(options.getLockLogFile())));
		
		LinkedList<Event> events = extractEvents(options.getLogFile(),lockLog);
		
		if (events != null)
			for(Event e: events) {
				System.err.println(e.toString());
			}
	}
	
	private static LinkedList<Event> extractEvents(String logBaseFileName, CSVOutputStream lockLog) throws FileNotFoundException {
		// EventListener listener = new ListenerCount(lockLog);
		EventListener listener = new ConcurrencyListener(lockLog);
		
		try {
			listener.processStart();
			for(File f: LogFiles.orderedLogFileList(logBaseFileName)) {
				Event.parseEvents(listener, new CSVInputStream(new FileInputStream(f)));
			}
			listener.processStop();
		} catch (EventParseException e) {
			System.err.println(e);
			e.printStackTrace();
		} catch (CSVInputStream.CSVException csve) {
			System.err.println(csve);
			csve.printStackTrace();
		}
		
		System.out.println(listener.toString());
		
		return null; // listener.events;
	}

	private static class CountListener extends EventListener {
		int start             = 0;
		int stop              = 0;
		int status            = 0;
		int time              = 0;
		int acquire           = 0;
		int release           = 0;
		int notify            = 0;
		int contended_enter   = 0;
		int contended_entered = 0;
		int wait              = 0;
		int waited            = 0;
		
		private CSVOutputStream os;
		private EventListener   chain;
		
		public CountListener() {
			this(null,null);
		}
		
		public CountListener(CSVOutputStream os) {
			this(os,null);
		}
		
		public CountListener(EventListener chain) {
			this(null,chain);
		}
		
		public CountListener(CSVOutputStream os, EventListener chain) {
			this.os    = os;
			this.chain = chain;
		}
		
		public void processStart() {
		}
		
		public void processEvent(Event e) {
			if (e.getLogPrefix()==LogTags.LOG_PREFIX_THREAD_START) this.start++;
			else if (e.getLogPrefix()==LogTags.LOG_PREFIX_THREAD_STOP) this.stop++;
			else if (getLogOn()) {
				if (e.getLogPrefix()==LogTags.LOG_PREFIX_MONITOR_ACQUIRE)                { this.acquire++; } 
				else if (e.getLogPrefix()==LogTags.LOG_PREFIX_MONITOR_RELEASE)           { this.release++; }
				else if (e.getLogPrefix()==LogTags.LOG_PREFIX_MONITOR_NOTIFY)            { this.notify++; }
				else if (e.getLogPrefix()==LogTags.LOG_PREFIX_MONITOR_CONTENTED_ENTER)   { this.contended_enter++; }
				else if (e.getLogPrefix()==LogTags.LOG_PREFIX_MONITOR_CONTENTED_ENTERED) { this.contended_entered++; }
				else if (e.getLogPrefix()==LogTags.LOG_PREFIX_MONITOR_WAIT)              { this.wait++; }
				else if (e.getLogPrefix()==LogTags.LOG_PREFIX_MONITOR_WAITED)            { this.waited++; }
			}
		}
		
		public void processStop() {
			if (this.os!=null) {
				this.os.write(LogTags.LOG_PREFIX_MONITOR_ACQUIRE);
				this.os.write(LogTags.LOG_PREFIX_MONITOR_RELEASE);
				this.os.write(LogTags.LOG_PREFIX_MONITOR_NOTIFY);
				this.os.write(LogTags.LOG_PREFIX_MONITOR_CONTENTED_ENTER);
				this.os.write(LogTags.LOG_PREFIX_MONITOR_CONTENTED_ENTERED);
				this.os.write(LogTags.LOG_PREFIX_MONITOR_WAIT);
				this.os.write(LogTags.LOG_PREFIX_MONITOR_WAITED);
				this.os.eol();

				this.os.write(""+this.acquire);
				this.os.write(""+this.release);
				this.os.write(""+this.notify);
				this.os.write(""+this.contended_enter);
				this.os.write(""+this.contended_entered);
				this.os.write(""+this.wait);
				this.os.write(""+this.waited);
				this.os.eol();
			}
		}
		
		public String toString() {
			return "Start:"+start+" Stop:"+stop+" Status:"+status+" Time:"+time+" Acquire:"+acquire+" Release:"+release+" ContendedEnter:"+contended_enter+" ContendedEntered:"+contended_entered+" Wait:"+wait+" Waited:"+waited;
		}
	}

	// Events                   | Notation
	// -------------------------+--------------------------------------------------
	//   MS: acquire            | A(t,x) - acquire a  lock on x by thread t
	//   ME: release            | R(t,x) - release of lock on x by thread t
	//   MN: notify             | N(t,x) - issue notify on lock x by thread t
	//   MC: contended enter    | C(t,x) - contended enter on lock x by thread t
	//   Mc: contended entered  | E(t,x) - contended entered on lock x by thread t
	//   MW: wait               | W(t,x) - wait on lock x by thread t
	//   Mw: waited             | D(t,x) - waited on lock x by thread t
	//
	// Rule: for events a and b the predicate
	//           a < b
	//       states that event a must occur before event b
	//
	// Rules:
	//   A(t,x)   < R(t,x)
	//   A(t_1,x) < C(t_2,x), R(t_1,x) < E(t_2,x) < R(t_2,x)
	//   W(t,x)   < D(t,x)
	//   D(t,x)   < W(t,x)|R(t,x)
	//   A(t,x)   < W(t,x)
	// 
	// Uninteresting sequences:
	//   A(t,x)+,R(t,x)+  
	//     can be eliminated as does not contribute anything interesting
	//
	private static class ConcurrencyListener extends EventListener {
		private TreeSet<Long> logonObjectLocks = new TreeSet<Long>(); 
		private TreeSet<Long> totalObjectLocks = new TreeSet<Long>(); 
		private VM vm = new VM();
		private TreeMap<Long,LinkedList<EventMonitorAcquire>> monitorAcquires = new TreeMap<Long,LinkedList<EventMonitorAcquire>>(); 
		
		private CSVOutputStream os;
		private EventListener   chain;
		
		public ConcurrencyListener() {
			this(null,null);
		}
		
		public ConcurrencyListener(CSVOutputStream os) {
			this(os,null);
		}
		
		public ConcurrencyListener(EventListener chain) {
			this(null,chain);
		}
		
		public ConcurrencyListener(CSVOutputStream os, EventListener chain) {
			this.os    = os;
			this.chain = chain;
		}
		
		public void processStart() { }
		
		public void processStop() { }
		
		public void processEvent(Event e) {
			if (this.chain != null) chain.processEvent(e);
			
			if (e.getLogPrefix()==LogTags.LOG_PREFIX_CLASS_PREPARE) {
				EventClassPrepare cE = (EventClassPrepare)e;
				VMClass vmClass = new VMClass(cE.getClassTag(), cE.getClassName(), cE.getTime());
				vmClass.setParent(vm.getVMClasses());
			} else if (getLogOn()) {
				// e.getLogPrefix()==LogTags.LOG_PREFIX_THREAD_START
				if (e.getLogPrefix()==LogTags.LOG_PREFIX_THREAD_START) {
					EventThreadStart tE = (EventThreadStart)e;
					
					VMClass vmClass = vm.getVMClasses().find(tE.getThreadClass());
					VMThreadState vmThreadState = new VMThreadState(tE.getThreadTag(),vmClass,tE.getThreadName(),tE.getTime());
					vmThreadState.setParent(vm.getVMThreadStates());
				} else
				// e.getLogPrefix()==LogTags.LOG_PREFIX_THREAD_STOP
				if (e.getLogPrefix()==LogTags.LOG_PREFIX_THREAD_STOP) {
					EventThreadStop tE = (EventThreadStop)e;
					VMThreadState t = vm.getVMThreadStates().find(tE.getThreadTag());
					t.setStop(tE.getTime());
				} else
				// e.getLogPrefix()==LogTags.LOG_PREFIX_THREAD_STATUS
				// e.getLogPrefix()==LogTags.LOG_PREFIX_THREAD_TIME
				if (e.getLogPrefix()==LogTags.LOG_PREFIX_THREAD_TIME) {
					EventThreadTime tE = (EventThreadTime)e;
					VMThreadState t = vm.getVMThreadStates().find(tE.getThreadTag());
					t.addTimeSlice(tE.getTime(), tE.getRunTime());
				} else
				// e.getLogPrefix()==LogTags.LOG_PREFIX_MONITOR_ACQUIRE
				if (e.getLogPrefix()==LogTags.LOG_PREFIX_MONITOR_ACQUIRE) {
					EventMonitorAcquire mS = (EventMonitorAcquire)e;
					LinkedList<EventMonitorAcquire> eSTree = monitorAcquires.get(mS.getObjectTag());
					if (eSTree==null) {
						eSTree = new LinkedList<EventMonitorAcquire>();
						monitorAcquires.put(mS.getObjectTag(), eSTree);
					}
					eSTree.addLast(mS);
					
					logonObjectLocks.add(mS.getObjectTag());
					totalObjectLocks.add(mS.getObjectTag());
				} else
				// e.getLogPrefix()==LogTags.LOG_PREFIX_MONITOR_RELEASE
				if (e.getLogPrefix()==LogTags.LOG_PREFIX_MONITOR_RELEASE) {
					EventMonitorRelease mE = (EventMonitorRelease)e;
					LinkedList<EventMonitorAcquire> eSTree = monitorAcquires.get(mE.getObjectTag());
					if (eSTree == null) log(mE);
					else {
						eSTree.removeLast();
						if (eSTree.isEmpty()) monitorAcquires.remove(mE.getObjectTag());
					}
					
					logonObjectLocks.add(mE.getObjectTag());
					totalObjectLocks.add(mE.getObjectTag());
				} else
				// e.getLogPrefix()==LogTags.LOG_PREFIX_MONITOR_CONTENTED_ENTER
				if (e.getLogPrefix()==LogTags.LOG_PREFIX_MONITOR_CONTENTED_ENTER ||
					e.getLogPrefix()==LogTags.LOG_PREFIX_MONITOR_CONTENTED_ENTERED ||
					e.getLogPrefix()==LogTags.LOG_PREFIX_MONITOR_WAIT ||
					e.getLogPrefix()==LogTags.LOG_PREFIX_MONITOR_WAITED ||
					e.getLogPrefix()==LogTags.LOG_PREFIX_MONITOR_NOTIFY) {
					EventMonitor eM = (EventMonitor)e;
					flush(eM.getObjectTag());
					log(eM);
					
					logonObjectLocks.add(eM.getObjectTag());
					totalObjectLocks.add(eM.getObjectTag());
				}
			} else {
				// e.getLogPrefix()==LogTags.LOG_PREFIX_MONITOR_ACQUIRE
				if (e.getLogPrefix()==LogTags.LOG_PREFIX_MONITOR_ACQUIRE ||
					e.getLogPrefix()==LogTags.LOG_PREFIX_MONITOR_RELEASE ||
					e.getLogPrefix()==LogTags.LOG_PREFIX_MONITOR_CONTENTED_ENTER ||
					e.getLogPrefix()==LogTags.LOG_PREFIX_MONITOR_CONTENTED_ENTERED ||
					e.getLogPrefix()==LogTags.LOG_PREFIX_MONITOR_WAIT ||
					e.getLogPrefix()==LogTags.LOG_PREFIX_MONITOR_WAITED ||
					e.getLogPrefix()==LogTags.LOG_PREFIX_MONITOR_NOTIFY) {
					totalObjectLocks.add(((EventMonitor)e).getObjectTag());
				}
			}
		}

		private void flush(long tag) {
			LinkedList<EventMonitorAcquire> eSTree = monitorAcquires.remove(tag);
			if (eSTree!=null) {
				for(EventMonitorAcquire e: eSTree)
					log(e);
			}
		}
		
		public String toString() {
			StringBuffer str = new StringBuffer();
			
			str.append("Classes:");
			str.append(EOL);
			VMClasses classes = vm.getVMClasses();
			
			for(Long tag: classes.getTags()) {
				str.append("  ");
				str.append(classes.find(tag).toString());
				str.append(EOL);
			}
			
			str.append("ThreadStates:");
			str.append(EOL);
			VMThreadStates threads = vm.getVMThreadStates();
			
			for(Long tag: threads.getTags()) {
				str.append(threads.find(tag).toString());
				str.append(EOL);
			}
			
			str.append("ThreadStates #");
			str.append(logonObjectLocks.size());
			str.append(" out of #");
			str.append(totalObjectLocks.size());
			str.append(EOL);
			for(Long tag: logonObjectLocks) {
				str.append("  ");
				str.append(tag);
				str.append(EOL);
			}
			
			return str.toString();
		}
		
		private void log(Event e) {
			if (os!=null)
				e.write(os);
		}
	}
}
