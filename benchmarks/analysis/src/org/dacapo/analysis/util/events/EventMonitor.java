package org.dacapo.analysis.util.events;

import org.dacapo.util.CSVInputStream;
import org.dacapo.util.CSVOutputStream;

public abstract class EventMonitor extends Event implements EventHasThread {

	private long threadTag;
	private long threadClassTag;
	private String threadClass;
	private String threadName;
	private long objectTag;
	private String objectClassName;

	protected EventMonitor(long time, long threadTag, String threadClassName, String threadName, long objectTag, String objectClassName) {
		super(time);
		this.threadTag = threadTag;
		this.threadClass = threadClassName;
		this.threadName = threadName;
		this.objectTag = objectTag;
		this.objectClassName = objectClassName;
	}
	   
	public long getThreadTag() { return threadTag; }
	public void setThreadTag(long threadTag) { this.threadTag = threadTag; }
	
	public long getThreadClassTag() { return threadClassTag; }
	public void setThreadClassTag(long threadClassTag) { this.threadClassTag = threadClassTag; }
	
	public String getThreadClass() { return threadClass; }
	public void setThreadClass(String threadClass) { this.threadClass = threadClass; }
	
	public String getThreadName() { return threadName; }
	public void setThreadName(String threadName) { this.threadName = threadName; }
	
	public long getObjectTag() { return objectTag; }
	
	public String getObjectClassName() { return objectClassName; }
	
	public String toString() { 
		return super.toString()+":"+getThreadTag()+":"+getThreadClassTag()+":"+getThreadClass()+":"+getThreadName()+":"+getObjectTag()+":"+getObjectClassName();
	}

	protected void writeEvent(CSVOutputStream os) {
		os.write(""+getTime());
		
		EventThread.write(os, this);

		os.write(""+objectTag);
		os.write(objectClassName);
	}
	
	EventMonitor(CSVInputStream is) throws CSVInputStream.CSVException, EventParseException {
		super(is);
		
		EventThread.read(is, this);
		
		this.objectTag       = is.nextFieldLong();
		this.objectClassName = is.nextFieldString();
	}
	
}
