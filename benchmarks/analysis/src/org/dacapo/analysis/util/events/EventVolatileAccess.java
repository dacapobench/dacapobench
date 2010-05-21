package org.dacapo.analysis.util.events;

import org.dacapo.analysis.util.CSVInputStream;
import org.dacapo.instrument.LogTags;

public class EventVolatileAccess extends Event {

	public  static final String TAG = LogTags.LOG_PREFIX_VOLATILE_ACCESS;
	
	private long   threadTag;
	private String threadClassName;
	private String threadName;
	
	private long   classTag;
	private long   fieldId;
	
	public EventVolatileAccess(long time, long threadTag, String threadClassName, String threadName,
							   long classTag, long fieldId) {
		super(time);
		this.threadTag = threadTag;
		this.threadClassName = threadClassName;
		this.threadName = threadName;
		this.classTag = classTag;
		this.fieldId = fieldId;
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
			
			long   classTag        = is.nextFieldLong();
			long   fieldId         = is.nextFieldPointer();
			
			if (is.numberOfFieldsLeft()==0) 
				return new EventVolatileAccess(time, threadTag, threadClassName, threadName, classTag, fieldId);
		} catch (Exception nfe) { }
		
		throw new EventParseException("format error "+TAG);
	}
}
