package org.dacapo.analysis.util.events;

import org.dacapo.analysis.util.CSVInputStream;
import org.dacapo.analysis.util.CSVOutputStream;
import org.dacapo.analysis.util.CSVInputStream.NoFieldAvailable;
import org.dacapo.analysis.util.CSVInputStream.ParseError;
import org.dacapo.instrument.LogTags;

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
