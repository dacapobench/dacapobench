package org.dacapo.analysis.util.events;

import org.dacapo.instrument.LogTags;
import org.dacapo.util.CSVInputStream;
import org.dacapo.util.CSVOutputStream;
import org.dacapo.util.CSVInputStream.NoFieldAvailable;
import org.dacapo.util.CSVInputStream.ParseError;

public class EventVolatileAccess extends Event implements EventHasThread {

	public  static final String TAG = LogTags.LOG_PREFIX_VOLATILE_ACCESS;
	
	private long   threadTag;
	private long   threadClassTag;
	private String threadClass;
	private String threadName;
	
	private long   classTag;
	private long   fieldId;
	
	public EventVolatileAccess(long time, long threadTag, long threadClassTag, String threadClass, String threadName,
							   long classTag, long fieldId) {
		super(time);
		this.threadTag = threadTag;
		this.threadClassTag = threadClassTag;
		this.threadClass = threadClass;
		this.threadName = threadName;
		this.classTag = classTag;
		this.fieldId = fieldId;
	}
	
	public String getLogPrefix() {
		return TAG;
	}
	
	protected void writeEvent(CSVOutputStream os) {
		os.write(""+getTime());
		
		EventThread.write(os, this);
		
		os.write(""+classTag);
		os.write(""+fieldId);
	}
	
	EventVolatileAccess(CSVInputStream is) throws NoFieldAvailable, ParseError, EventParseException {
		super(is);
		
		EventThread.read(is, this);
		
		classTag  = is.nextFieldLong();
		fieldId   = is.nextFieldPointer();
		
		if (is.numberOfFieldsLeft()!=0) 
			throw new EventParseException("additional fields", null);
	}

	public long getThreadTag() { return threadTag; }
	public void setThreadTag(long threadTag) { this.threadTag = threadTag; }

	public long getThreadClassTag() { return threadClassTag; }
	public void setThreadClassTag(long threadClassTag) { this.threadClassTag = threadClassTag; }

	public String getThreadClass() { return threadClass; }
	public void setThreadClass(String threadClass) { this.threadClass = threadClass; }

	public String getThreadName() { return threadName; }
	public void setThreadName(String threadName) { this.threadName = threadName; }

}
