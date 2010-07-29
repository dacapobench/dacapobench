package org.dacapo.analysis.util.events;

import org.dacapo.instrument.LogTags;
import org.dacapo.util.CSVInputStream;
import org.dacapo.util.CSVOutputStream;
import org.dacapo.util.CSVInputStream.NoFieldAvailable;
import org.dacapo.util.CSVInputStream.ParseError;

public class EventHeapReport extends Event implements EventHasThread {
	
	public  static final String TAG = LogTags.LOG_PREFIX_HEAP_REPORT;
		
	private long   threadTag;
	private long   threadClassTag;
	private String threadClass;
	private String threadName;
	private long   used;
	private long   free;
	private long   total;
	private long   max;
	
	public EventHeapReport(long time, 
						   long threadTag, long threadClassTag, String threadClass, String threadName,
						   long used, long free,      long   total,           long   max) {
		super(time);
		this.threadTag = threadTag;
		this.threadClassTag = threadClassTag;
		this.threadClass = threadClass;
		this.threadName = threadName;
		this.used = used;
		this.free = free;
		this.total = total;
		this.max = max;
	}
	
	public String getLogPrefix() {
		return TAG;
	}

	public long getThreadTag() { return threadTag; }
	public void setThreadTag(long threadTag) { this.threadTag = threadTag; }
	
	public long getThreadClassTag() { return threadClassTag; }
	public void setThreadClassTag(long threadClassTag) { this.threadClassTag = threadClassTag; }
	
	public String getThreadClass() { return threadClass; }
	public void   setThreadClass(String threadClass) { this.threadClass = threadClass; }
	
	public String getThreadName() { return threadName; }
	public void setThreadName(String threadName) { this.threadName = threadName; }
	
	public long getUsed() { return used; }
	
	public long getFree() { return free; }
	
	public long getTotal() { return total; }
	
	public long getMax() { return max; }

	protected void writeEvent(CSVOutputStream os) {
		os.write(""+getTime());
		
		EventThread.write(os, this);
		
		os.write(""+used);
		os.write(""+free);
		os.write(""+total);
		os.write(""+max);
	}
	
	EventHeapReport(CSVInputStream is) throws NoFieldAvailable, ParseError, EventParseException {
		super(is);
		
		EventThread.read(is, this);
		
		used              = is.nextFieldLong();
		free              = is.nextFieldLong();
		total             = is.nextFieldLong();
		max               = is.nextFieldLong();
		
		if (is.numberOfFieldsLeft()!=0 && this instanceof EventHeapReport) 
			throw new EventParseException("additional fields", null);
		

	}
}
