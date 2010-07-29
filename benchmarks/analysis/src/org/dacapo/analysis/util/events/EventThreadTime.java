package org.dacapo.analysis.util.events;

import org.dacapo.instrument.LogTags;
import org.dacapo.util.CSVInputStream;
import org.dacapo.util.CSVOutputStream;
import org.dacapo.util.CSVInputStream.NoFieldAvailable;
import org.dacapo.util.CSVInputStream.ParseError;

public class EventThreadTime extends Event {

	public  static final String TAG = LogTags.LOG_PREFIX_THREAD_TIME;
	
	private long index;
	private long threadTag;
	private long runTime;
	
	public EventThreadTime(long time, long index, long threadTag, long runTime) {
		super(time);
		this.index = index;
		this.threadTag = threadTag;
		this.runTime = runTime;
	}
	
	public String getLogPrefix() { return TAG; }
	
	public long getIndex() { return index; }
	
	public long getThreadTag() { return threadTag; }
	
	public long getRunTime() { return runTime; }
	
	public String toString() { 
		return super.toString()+":"+getIndex()+":"+getThreadTag()+":"+getRunTime();
	}

	protected void writeEvent(CSVOutputStream os) {
		os.write(""+getTime());
		os.write(""+index);
		os.write(""+threadTag);
		os.write(""+runTime);
	}
	
	EventThreadTime(CSVInputStream is) throws NoFieldAvailable, ParseError, EventParseException {
		super(is);
		
		index           = is.nextFieldLong();
		threadTag       = is.nextFieldLong();
		runTime         = is.nextFieldLong();
		
		if (is.numberOfFieldsLeft()!=0 && this instanceof EventThreadTime) 
			throw new EventParseException("additional fields", null);
	}
}
