package org.dacapo.analysis.util.events;


import org.dacapo.instrument.LogTags;
import org.dacapo.util.CSVInputStream;
import org.dacapo.util.CSVOutputStream;
import org.dacapo.util.CSVInputStream.CSVException;

public class EventMonitorContendedEnter extends EventMonitor {

	public  static final String TAG = LogTags.LOG_PREFIX_MONITOR_CONTENTED_ENTER;
	
	public EventMonitorContendedEnter(long time, long threadTag, String threadClassName, String threadName, long objectTag, String objectClassName) {
		super(time, threadTag, threadClassName, threadName, objectTag, objectClassName);
	}
	
	public String getLogPrefix() { return TAG; }
	
	protected void writeEvent(CSVOutputStream os) { super.writeEvent(os); }
	
	EventMonitorContendedEnter(CSVInputStream is) throws CSVException, EventParseException {
		super(is);
		
		if (is.numberOfFieldsLeft()!=0 && this instanceof EventMonitorContendedEnter) 
			throw new EventParseException("additional fields", null);
	}
}
