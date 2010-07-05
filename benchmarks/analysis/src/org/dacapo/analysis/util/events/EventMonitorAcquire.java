package org.dacapo.analysis.util.events;

import org.dacapo.analysis.util.CSVInputStream;
import org.dacapo.analysis.util.CSVOutputStream;
import org.dacapo.analysis.util.CSVInputStream.CSVException;
import org.dacapo.instrument.LogTags;

public class EventMonitorAcquire extends EventMonitor {

	public  static final String TAG = LogTags.LOG_PREFIX_MONITOR_ACQUIRE;
	
	public EventMonitorAcquire(long time, long threadTag, String threadClassName, String threadName, long objectTag, String objectClassName) {
		super(time, threadTag, threadClassName, threadName, objectTag, objectClassName);
	}
	
	public String getLogPrefix() { return TAG; }
	
	protected void writeEvent(CSVOutputStream os) { super.writeEvent(os); }
	
	EventMonitorAcquire(CSVInputStream is) throws CSVException, EventParseException {
		super(is);
		
		if (is.numberOfFieldsLeft()!=0 && this instanceof EventMonitorAcquire) 
			throw new EventParseException("additional fields", null);
	}
}
