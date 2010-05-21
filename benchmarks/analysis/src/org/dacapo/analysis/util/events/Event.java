package org.dacapo.analysis.util.events;

import java.util.Collection;
import java.util.LinkedList;

import org.dacapo.analysis.util.CSVInputStream;
import org.dacapo.analysis.util.CSVInputStream.CSVException;
import org.dacapo.instrument.LogTags;

public abstract class Event {

	private long time;
	
	public Event(long time) {
		this.time = time;
	}
	
	public abstract String getLogPrefix();

	public long getTime() { return time; }
	
	public static LinkedList<Event> parseEvents(CSVInputStream is) {
		return parseEvents(is, true);
	}
	
	public static LinkedList<Event> parseEvents(CSVInputStream is, boolean loggedOnly) {
		return parseEvents(is, true, null);
	}
	
	public static LinkedList<Event> parseEvents(CSVInputStream is, boolean loggedOnly, Collection<String> eventSet) {
		LinkedList<Event> events = new LinkedList<Event>();
		
		boolean logPeriod = false;
		
		try {
			while (is.nextRow()) {
				Event event = parseEvent(is, eventSet);

				if (event != null) {
					if (EventStart.TAG.equals(event.getLogPrefix())) 
						logPeriod = true;
					else if (EventStop.TAG.equals(event.getLogPrefix()))  
						logPeriod = false;
					else if (!loggedOnly || logPeriod) {
						if (event != null && (eventSet == null || eventSet.contains(event.getLogPrefix())))
							events.addLast(event);
					}
				}
			}
		} catch (EventParseException epe) {
		} catch (CSVException csve) {
		}
		
		return events;
	}
	
	public static Event parseEvent(CSVInputStream is, Collection<String> eventSet) throws EventParseException, CSVException {
		String eventTag = is.nextFieldString();
		
		if (eventSet!=null && !eventSet.contains(eventTag))
			return null;
		
		if (EventStart.TAG.equals(eventTag))
			return EventStart.parse(is);
		if (EventStop.TAG.equals(eventTag))
			return EventStop.parse(is);

		if (EventAllocation.TAG.equals(eventTag))
			return EventAllocation.parse(is);
		if (EventFree.TAG.equals(eventTag))
			return EventFree.parse(is);
		if (EventGC.TAG.equals(eventTag))
			return EventGC.parse(is);
		if (EventHeapReport.TAG.equals(eventTag))
			return EventHeapReport.parse(is);

		if (EventCallChainFrame.TAG.equals(eventTag))
			return EventCallChainFrame.parse(is);
		if (EventCallChainStart.TAG.equals(eventTag))
			return EventCallChainStart.parse(is);
		if (EventCallChainStop.TAG.equals(eventTag))
			return EventCallChainStop.parse(is);
		if (EventMethodEnter.TAG.equals(eventTag))
			return EventMethodEnter.parse(is);
		if (EventMethodExit.TAG.equals(eventTag))
			return EventMethodExit.parse(is);

		if (EventClassInitialization.TAG.equals(eventTag))
			return EventClassInitialization.parse(is);
		if (EventClassPrepare.TAG.equals(eventTag))
			return EventClassPrepare.parse(is);
		if (EventMethodPrepare.TAG.equals(eventTag))
			return EventMethodPrepare.parse(is);

		if (EventException.TAG.equals(eventTag))
			return EventException.parse(is);
		if (EventExceptionCatch.TAG.equals(eventTag))
			return EventExceptionCatch.parse(is);

		if (EventMonitorAcquire.TAG.equals(eventTag))
			return EventMonitorAcquire.parse(is);
		if (EventMonitorContendedEnter.TAG.equals(eventTag))
			return EventMonitorContendedEntered.parse(is);
		if (EventMonitorRelease.TAG.equals(eventTag))
			return EventMonitorRelease.parse(is);
		if (EventMonitorWait.TAG.equals(eventTag))
			return EventMonitorWait.parse(is);
		if (EventMonitorWaited.TAG.equals(eventTag))
			return EventMonitorWaited.parse(is);

		if (EventThreadStart.TAG.equals(eventTag))
			return EventThreadStart.parse(is);
		if (EventThreadStatus.TAG.equals(eventTag))
			return EventThreadStatus.parse(is);
		if (EventThreadStop.TAG.equals(eventTag))
			return EventThreadStop.parse(is);
		if (EventThreadTime.TAG.equals(eventTag))
			return EventThreadTime.parse(is);

		if (EventVolatile.TAG.equals(eventTag))
			return EventVolatile.parse(is);
		if (EventVolatileAccess.TAG.equals(eventTag))
			return EventVolatileAccess.parse(is);
		
		throw new EventParseException(eventTag);
	}
}
