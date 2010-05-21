package org.dacapo.analysis.util.events;

import org.dacapo.analysis.util.CSVInputStream;
import org.dacapo.instrument.LogTags;

public class EventMethodEnter extends Event {
	
	public  static final String TAG = LogTags.LOG_PREFIX_METHOD_ENTER;
		
	private long   threadTag;
	private String threadClassName;
	private String threadName;
	
	private long   methodId;

	public EventMethodEnter(long time, long threadTag, String threadClassName, String threadName, long methodId) {
		super(time);
		this.threadTag = threadTag;
		this.threadClassName = threadClassName;
		this.threadName = threadName;
		this.methodId = methodId;
	}
	
	public String getLogPrefix() {
		return TAG;
	}

	static Event parse(CSVInputStream is) throws EventParseException {
		try {
			long time              = is.nextFieldLong();

			long threadTag         = is.nextFieldLong();
			String threadClassName = is.nextFieldString();
			String threadName      = is.nextFieldString();

			long methodId          = is.nextFieldPointer();
			
			if (is.numberOfFieldsLeft()==0) 
				return new EventMethodEnter(time, threadTag, threadClassName, threadName,
										    methodId);
			
		} catch (Exception nfe) { }
		
		throw new EventParseException("format error "+TAG);
	}
}
