package org.dacapo.analysis.util.events;

import org.dacapo.instrument.LogTags;

public abstract class EventListener {

	private boolean logOn = false;
	
	public final void event(Event e) {
		if (e==null) return;
		
		if (e.getLogPrefix()==LogTags.LOG_PREFIX_START) {
			logOn = true;
		} else if (e.getLogPrefix()==LogTags.LOG_PREFIX_STOP) {
			logOn = false;
		}
		processEvent(e);
	}
	
	public boolean getLogOn() { return logOn; }
	
	public abstract void processStart();
	
	public abstract void processEvent(Event e);
	
	public abstract void processStop();
}
