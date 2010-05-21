package org.dacapo.analysis.util.events;

import org.dacapo.analysis.util.CSVInputStream;
import org.dacapo.instrument.LogTags;

public class EventHeapReport extends Event {
	
	public  static final String TAG = LogTags.LOG_PREFIX_HEAP_REPORT;
		
	private long   threadTag;
	private String threadClassName;
	private String threadName;
	private long   used;
	private long   free;
	private long   total;
	private long   max;
	
	public EventHeapReport(long time, long threadTag, String threadClassName, String threadName,
						   long used, long free,      long   total,           long   max) {
		super(time);
		this.threadTag = threadTag;
		this.threadClassName = threadClassName;
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
	
	public String getThreadClassName() { return threadClassName; }
	
	public String getThreadName() { return threadName; }
	
	public long getUsed() { return used; }
	
	public long getFree() { return free; }
	
	public long getTotal() { return total; }
	
	public long getMax() { return max; }
	
	static Event parse(CSVInputStream is) throws EventParseException {
		try {
			long time              = is.nextFieldLong();
			
			long threadTag         = is.nextFieldLong();
			String threadClassName = is.nextFieldString();
			String threadName      = is.nextFieldString();

			long used              = is.nextFieldLong();
			long free              = is.nextFieldLong();
			long total             = is.nextFieldLong();
			long max               = is.nextFieldLong();
			
			if (is.numberOfFieldsLeft()==0) 
				return new EventHeapReport(time, threadTag, threadClassName, threadName,
										   used, free,      total,           max);
			
		} catch (Exception nfe) { }
		
		throw new EventParseException("format error "+TAG);
	}
}
