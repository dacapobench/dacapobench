package org.dacapo.analysis.util.events;

import org.dacapo.analysis.util.CSVInputStream;
import org.dacapo.instrument.LogTags;

public class EventThreadStatus extends Event {

	public  static final String TAG = LogTags.LOG_PREFIX_THREAD_STATUS;
	
	private long threadTag;
	
	public EventThreadStatus(long time, long threadTag) {
		super(time);
		this.threadTag = threadTag;
	}
	
	public String getLogPrefix() {
		return TAG;
	}
	
	static Event parse(CSVInputStream is) throws EventParseException {
		try {
			long time              = is.nextFieldLong();

			long threadTag         = is.nextFieldLong();

			if (is.numberOfFieldsLeft()==0) 
				return new EventThreadStatus(time, threadTag);
		} catch (Exception nfe) { }
		
		throw new EventParseException("format error "+TAG);
	}
}
