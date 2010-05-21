package org.dacapo.analysis.util.events;

import org.dacapo.analysis.util.CSVInputStream;
import org.dacapo.instrument.LogTags;

public class EventClassPrepare extends Event {

	public  static final String TAG = LogTags.LOG_PREFIX_CLASS_PREPARE;
	
	private long   classTag;
	private String className;
	
	public EventClassPrepare(long time, long classTag, String className) {
		super(time);
		this.classTag  = classTag;
		this.className = className;
	}

	public String getLogPrefix() {
		return TAG;
	}
	
	public long getClassTag() { return classTag; }
	
	public String getClassName() { return className; }

	static Event parse(CSVInputStream is) throws EventParseException {
		try {
			long time              = is.nextFieldLong();

			long classTag          = is.nextFieldLong();
			String className       = is.nextFieldString(); 

			if (is.numberOfFieldsLeft()==0) 
				return new EventClassPrepare(time, classTag, className);
			
		} catch (Exception nfe) { }
		
		throw new EventParseException("format error "+TAG);
	}
}
