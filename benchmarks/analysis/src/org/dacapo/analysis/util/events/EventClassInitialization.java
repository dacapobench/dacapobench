package org.dacapo.analysis.util.events;

import org.dacapo.analysis.util.CSVInputStream;
import org.dacapo.instrument.LogTags;

public class EventClassInitialization extends Event {

	public  static final String TAG = LogTags.LOG_PREFIX_CLASS_INITIALIZATION;
	
	private long threadTag;
	private String threadClassName;
	private String threadName;
	private String className;
	
	public EventClassInitialization(long time, 
									long threadTag, String threadClassName, String threadName,
									String className) {
		super(time);
		this.threadTag   = threadTag;
		this.threadClassName = threadClassName;
		this.threadName  = threadName;
		this.className   = className;
	}
	
	public String getLogPrefix() {
		return TAG;
	}
	
	public long getThreadTag() { return threadTag; }
	
	public String getThreadClassName() { return threadClassName; }
	
	public String getThreadName() { return threadName; }
	
	public String getClassName() { return className; }
	
	static Event parse(CSVInputStream is) throws EventParseException {
		try {
			long time              = is.nextFieldLong();

			long threadTag         = is.nextFieldLong();
			String threadClassName = is.nextFieldString(); 
			String threadName      = is.nextFieldString(); 

			String className       = is.nextFieldString();
			
			if (is.numberOfFieldsLeft()==0) 
				return new EventClassInitialization(time, threadTag, threadClassName, threadName, className);
			
		} catch (Exception nfe) { }
		
		throw new EventParseException("format error "+TAG);
	}
}
