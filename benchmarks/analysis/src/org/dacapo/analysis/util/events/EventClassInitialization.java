package org.dacapo.analysis.util.events;

import org.dacapo.analysis.util.CSVInputStream;
import org.dacapo.analysis.util.CSVOutputStream;
import org.dacapo.analysis.util.CSVInputStream.NoFieldAvailable;
import org.dacapo.analysis.util.CSVInputStream.ParseError;
import org.dacapo.instrument.LogTags;

public class EventClassInitialization extends Event implements EventHasThread {

	public  static final String TAG = LogTags.LOG_PREFIX_CLASS_INITIALIZATION;
	
	private long threadTag;
	private long threadClassTag;
	private String threadClass;
	private String threadName;
	private String className;
	
	public EventClassInitialization(long time, 
									long threadTag, long threadClassTag, String threadClassName, String threadName,
									String className) {
		super(time);
		this.threadTag   = threadTag;
		this.threadClassTag = threadClassTag;
		this.threadClass = threadClassName;
		this.threadName  = threadName;
		this.className   = className;
	}
	
	
	protected void writeEvent(CSVOutputStream os) {
		os.write(""+getTime());
		
		EventThread.write(os, this);

		os.write(className);
	}

	public String getLogPrefix() {
		return TAG;
	}
	
	public long getThreadTag() { return threadTag; }
	public void setThreadTag(long threadTag) { this.threadTag = threadTag; }
	
	public long getThreadClassTag() { return threadClassTag; }
	public void setThreadClassTag(long threadClassTag) { this.threadClassTag = threadClassTag; }
	
	public String getThreadClass() { return threadClass; }
	public void setThreadClass(String threadClass) { this.threadClass = threadClass; }
	
	public String getThreadName() { return threadName; }
	public void setThreadName(String threadName) { this.threadName = threadName; }
	
	public String getClassName() { return className; }
	
	EventClassInitialization(CSVInputStream is) throws EventParseException, NoFieldAvailable, ParseError {
		super(is);
		
		EventThread.read(is, this);
		
		className       = is.nextFieldString();
		
		if (is.numberOfFieldsLeft()!=0 && this instanceof EventClassInitialization)
			throw new EventParseException("additional field",null);
	}
}
