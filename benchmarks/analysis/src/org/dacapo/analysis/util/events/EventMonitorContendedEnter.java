package org.dacapo.analysis.util.events;

import org.dacapo.analysis.util.CSVInputStream;
import org.dacapo.instrument.LogTags;

public class EventMonitorContendedEnter extends Event {

	public  static final String TAG = LogTags.LOG_PREFIX_MONITOR_CONTENTED_ENTER;
	
	private long threadTag;
	private String threadClassName;
	private String threadName;
	private long objectTag;
	private String objectClassName;
	
	public EventMonitorContendedEnter(long time, long threadTag, String threadClassName, String threadName,
			   long objectTag, String objectClassName) {
		super(time);
		this.threadTag = threadTag;
		this.threadClassName = threadClassName;
		this.threadName = threadName;
		this.objectTag = objectTag;
		this.objectClassName = objectClassName;
	}
	
	public String getLogPrefix() {
		return TAG;
	}

	static Event parse(CSVInputStream is) throws EventParseException {
		try {
			long time              = is.nextFieldLong();

			long   threadTag       = is.nextFieldLong();
			String threadClassName = is.nextFieldString();
			String threadName      = is.nextFieldString();
			
			long   objectTag       = is.nextFieldLong();
			String objectClassName = is.nextFieldString();

			if (is.numberOfFieldsLeft()==0) 
				return new EventMonitorContendedEnter(time, threadTag, threadClassName, threadName,
    					   objectTag, objectClassName);
		} catch (Exception nfe) { }
		
		throw new EventParseException("format error "+TAG);
	}
}
