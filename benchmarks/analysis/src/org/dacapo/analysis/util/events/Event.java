package org.dacapo.analysis.util.events;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Vector;

import org.dacapo.instrument.LogTags;
import org.dacapo.util.CSVInputStream;
import org.dacapo.util.CSVOutputStream;
import org.dacapo.util.CSVInputStream.CSVException;
import org.dacapo.util.CSVInputStream.NoFieldAvailable;
import org.dacapo.util.CSVInputStream.ParseError;

public abstract class Event {

	private long time;
	
	protected Event() {
		this(0);
	}
	
	protected Event(long time) {
		this.time = time;
	}
	
	public abstract String getLogPrefix();

	public void setTime(long time) { this.time = time; }
	public long getTime() { return time; }

	public String toString() {
		return getLogPrefix()+":"+getTime();
	}
	
	public void write(CSVOutputStream os) {
		os.write(getLogPrefix());
		writeEvent(os);
		os.eol();
	}
	
	protected abstract void writeEvent(CSVOutputStream os);
	
	public static void parseEvents(EventListener listener, CSVInputStream is) throws EventParseException, CSVException {
		parseEvents(listener, is, null);
	}
	
	public static void parseEvents(EventListener listener, CSVInputStream is, EventErrorListener errorListener) throws EventParseException, CSVException {
		boolean logPeriod = false;
		
		try {
			while (is.nextRow()) {
				Event event = null;
				if (errorListener == null)
					event = parseEvent(is);
				else {
					try {
						event = parseEvent(is);
					} catch (CSVException cE) {
						errorListener.handle(cE);
					} catch (EventParseException ePE) {
						errorListener.handle(ePE);
					}
				}
					
				if (event != null) {
					listener.event(event);
				}
			}
		} catch (EventParseException epe) {
			Vector<String> row = is.getAllFields();
			StringBuffer sb = new StringBuffer("Fields:");
			
			for(String field: row) {
				sb.append(System.getProperty("line.separator"));
				sb.append("  ");
				sb.append(field);
			}
			
			throw new EventParseException(sb.toString(), epe);
		} catch (CSVException epe) {
			Vector<String> row = is.getAllFields();
			StringBuffer sb = new StringBuffer("Fields:");
			
			for(String field: row) {
				sb.append(System.getProperty("line.separator"));
				sb.append("  ");
				sb.append(field);
			}
			
			throw new EventParseException(sb.toString(), epe);
		}
	}
	
	public static Event parseEvent(CSVInputStream is) throws EventParseException, CSVException {
		String eventTag = is.nextFieldString();
		
		if (eventTag==null) return null;

		// never ignore start and stop tags
		if (EventStart.TAG.equals(eventTag))
			return new EventStart(is);
		else if (EventStop.TAG.equals(eventTag))
			return new EventStop(is);

		else if (EventAllocation.TAG.equals(eventTag))
			return new EventAllocation(is);
		else if (EventPointerChange.TAG.equals(eventTag))
			return new EventPointerChange(is);
		else if (EventStaticPointerChange.TAG.equals(eventTag))
			return new EventStaticPointerChange(is);
		else if (EventFree.TAG.equals(eventTag))
			return new EventFree(is);
		else if (EventGC.TAG.equals(eventTag))
			return new EventGC(is);
		else if (EventHeapReport.TAG.equals(eventTag))
			return new EventHeapReport(is);

		else if (EventCallChainFrame.TAG.equals(eventTag))
			return new EventCallChainFrame(is);
		else if (EventCallChainStart.TAG.equals(eventTag))
			return new EventCallChainStart(is);
		else if (EventCallChainStop.TAG.equals(eventTag))
			return new EventCallChainStop(is);
		else if (EventMethodEnter.TAG.equals(eventTag))
			return new EventMethodEnter(is);
		else if (EventMethodExit.TAG.equals(eventTag))
			return new EventMethodExit(is);

		else if (EventClassInitialization.TAG.equals(eventTag))
			return new EventClassInitialization(is);
		else if (EventClassPrepare.TAG.equals(eventTag))
			return new EventClassPrepare(is);
		else if (EventMethodPrepare.TAG.equals(eventTag))
			return new EventMethodPrepare(is);

		else if (EventException.TAG.equals(eventTag))
			return new EventException(is);
		else if (EventExceptionCatch.TAG.equals(eventTag))
			return new EventExceptionCatch(is);

		else if (EventMonitorAcquire.TAG.equals(eventTag))
			return new EventMonitorAcquire(is);
		else if (EventMonitorRelease.TAG.equals(eventTag))
			return new EventMonitorRelease(is);
		else if (EventMonitorContendedEnter.TAG.equals(eventTag))
			return new EventMonitorContendedEnter(is);
		else if (EventMonitorContendedEntered.TAG.equals(eventTag))
			return new EventMonitorContendedEntered(is);
		else if (EventMonitorWait.TAG.equals(eventTag))
			return new EventMonitorWait(is);
		else if (EventMonitorWaited.TAG.equals(eventTag))
			return new EventMonitorWaited(is);
		else if (EventMonitorNotify.TAG.equals(eventTag))
			return new EventMonitorNotify(is);

		else if (EventThreadStart.TAG.equals(eventTag))
			return new EventThreadStart(is);
		else if (EventThreadStatus.TAG.equals(eventTag))
			return new EventThreadStatus(is);
		else if (EventThreadStop.TAG.equals(eventTag))
			return new EventThreadStop(is);
		else if (EventThreadTime.TAG.equals(eventTag))
			return new EventThreadTime(is);

		else if (EventVolatile.TAG.equals(eventTag))
			return new EventVolatile(is);
		else if (EventVolatileAccess.TAG.equals(eventTag))
			return new EventVolatileAccess(is);
		
		throw new EventParseException("No such tag: "+eventTag,null);
	}
	
	
	Event(CSVInputStream is) throws NoFieldAvailable, ParseError {
		this.time = is.nextFieldLong();
	}
}
