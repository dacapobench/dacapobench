package org.dacapo.analysis.util.events;

import org.dacapo.analysis.util.CSVInputStream;
import org.dacapo.instrument.LogTags;

public class EventAllocation extends Event {

	public  static final String TAG = LogTags.LOG_PREFIX_ALLOCATION;
	
	private long   objectTag;
	private long   classTag;
	private String className;
	private long   threadTag;
	private String threadClassName;
	private String threadName;
	private long   size;
	
	public EventAllocation(long time,      long objectTag,      
						   long classTag,  String className,
						   long threadTag, String threadClassName, String threadName,
						   long size) {
		super(time);
		this.objectTag       = objectTag;
		this.classTag        = classTag;
		this.className       = className;
		this.threadTag       = threadTag;
		this.threadClassName = threadClassName;
		this.threadName      = threadName;
		this.size            = size;
	}

	public String getLogPrefix() {
		return TAG;
	}
	
	public long getObjectTag() { return objectTag; }
	
	public long getClassTag() { return classTag; }
	
	public String getClassName() { return className; }
	
	public long getThreadTag() { return threadTag; }
	
	public String getThreadClassName() { return threadClassName; }
	
	public String getThreadName() { return threadName; }
	
	public long getSize() { return size; }
	
	static Event parse(CSVInputStream is) throws EventParseException {
		try {
			long time              = is.nextFieldLong();
			long objectTag         = is.nextFieldLong();

			long classTag          = is.nextFieldLong();
			String className       = is.nextFieldString();

			long threadTag         = is.nextFieldLong();
			String threadClassName = is.nextFieldString(); 
			String threadName      = is.nextFieldString(); 
			
			long size              = is.nextFieldLong();
			
			if (is.numberOfFieldsLeft()==0) 
				return new EventAllocation(time, objectTag,
                                           classTag, className,
                                           threadTag, threadClassName, threadName,
									       size);
			
		} catch (Exception nfe) { }
		
		throw new EventParseException("format error "+TAG);
	}
}
