package org.dacapo.analysis.util.events;

import org.dacapo.analysis.util.CSVInputStream;
import org.dacapo.instrument.LogTags;

public class EventThreadStart extends Event {

	public  static final String TAG = LogTags.LOG_PREFIX_THREAD_START;
	
	private long threadTag;
	private String threadClassName;
	private String threadName;
	
	public EventThreadStart(long time, long threadTag, String threadClassName, String threadName) {
		super(time);
		this.threadTag = threadTag;
		this.threadClassName = threadClassName;
		this.threadName = threadName;
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
			
			if (is.numberOfFieldsLeft()==0) 
				return new EventThreadStart(time, threadTag, threadClassName, threadName);
		} catch (Exception nfe) { }
		
		throw new EventParseException("format error "+TAG);
	}
}
