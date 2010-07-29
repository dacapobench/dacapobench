package org.dacapo.analysis.util.events;

import org.dacapo.instrument.LogTags;
import org.dacapo.util.CSVInputStream;
import org.dacapo.util.CSVOutputStream;
import org.dacapo.util.CSVInputStream.NoFieldAvailable;
import org.dacapo.util.CSVInputStream.ParseError;

public class EventMethodEnter extends Event implements EventHasThread {
	
	public  static final String TAG = LogTags.LOG_PREFIX_METHOD_ENTER;
		
	private long   threadTag;
	private long   threadClassTag;
	private String threadClass;
	private String threadName;
	
	private long   methodId;

	public EventMethodEnter(long time, long threadTag, String threadClass, String threadName, long methodId) {
		super(time);
		this.threadTag = threadTag;
		this.threadClass = threadClass;
		this.threadName = threadName;
		this.methodId = methodId;
	}
	
	public String getLogPrefix() { return TAG; }
	
	public long getThreadTag() { return threadTag; }
	public void setThreadTag(long threadTag) { this.threadTag = threadTag; }
	
	public long getThreadClassTag() { return threadClassTag; }
	public void setThreadClassTag(long threadClassTag) { this.threadClassTag = threadClassTag; }
	
	public String getThreadClass() { return threadClass; }
	public void setThreadClass(String threadClass) { this.threadClass = threadClass; }
	
	public String getThreadName() { return threadName; }
	public void setThreadName(String threadName) { this.threadName = threadName; }
	
	public String toString() { return super.toString()+":"+getThreadTag()+":"+getThreadClassTag()+":"+getThreadClass()+":"+getThreadName(); }

	protected void writeEvent(CSVOutputStream os) {
		os.write(""+getTime());
		
		EventThread.write(os, this);

		os.write(""+methodId);
	}
	
	EventMethodEnter(CSVInputStream is) throws EventParseException, NoFieldAvailable, ParseError {
		super(is);
		
		EventThread.read(is, this);

		this.methodId          = is.nextFieldPointer();
		
		if (is.numberOfFieldsLeft()!=0 && this instanceof EventMethodEnter) 
			throw new EventParseException("additional fields", null);
	}
}
