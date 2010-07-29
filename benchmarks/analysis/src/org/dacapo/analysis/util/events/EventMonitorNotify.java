package org.dacapo.analysis.util.events;


import org.dacapo.instrument.LogTags;
import org.dacapo.util.CSVInputStream;
import org.dacapo.util.CSVOutputStream;
import org.dacapo.util.CSVInputStream.CSVException;

public class EventMonitorNotify extends EventMonitor {

	public  static final String TAG = LogTags.LOG_PREFIX_MONITOR_NOTIFY;
	
	public EventMonitorNotify(long time, long threadTag, String threadClassName, String threadName, long objectTag, String objectClassName) {
		super(time, threadTag, threadClassName, threadName, objectTag, objectClassName);
	}
	
	public String getLogPrefix() { return TAG; }
	
	protected void writeEvent(CSVOutputStream os) { super.writeEvent(os); }
	
	EventMonitorNotify(CSVInputStream is) throws CSVException, EventParseException {
		super(is);
		
		if (is.numberOfFieldsLeft()!=0 && this instanceof EventMonitorNotify) 
			throw new EventParseException("additional fields", null);
	}
}

