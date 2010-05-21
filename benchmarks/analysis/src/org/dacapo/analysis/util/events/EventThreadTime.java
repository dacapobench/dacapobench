package org.dacapo.analysis.util.events;

import org.dacapo.analysis.util.CSVInputStream;
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
	
	public String getLogPrefix() {
		return TAG;
	}

	static Event parse(CSVInputStream is) throws EventParseException {
		try {
			long time              = is.nextFieldLong();

			long   index           = is.nextFieldLong();
			long   threadTag       = is.nextFieldLong();
			long   runTime         = is.nextFieldLong();
			
			if (is.numberOfFieldsLeft()==0) 
				return new EventThreadTime(time, index, threadTag, runTime);
		} catch (Exception nfe) { }
		
		throw new EventParseException("format error "+TAG);
	}
}
