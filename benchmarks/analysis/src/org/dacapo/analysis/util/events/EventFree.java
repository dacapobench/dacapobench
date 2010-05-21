package org.dacapo.analysis.util.events;

import org.dacapo.analysis.util.CSVInputStream;
import org.dacapo.instrument.LogTags;

public class EventFree extends Event {

	public  static final String TAG = LogTags.LOG_PREFIX_FREE;
	
	private long objectTag;
	
	public EventFree(long time, long objectTag) {
		super(time);
		this.objectTag = objectTag;
	}

	public String getLogPrefix() {
		return TAG;
	}

	public long getObjectTag() { return objectTag; }
	
	static Event parse(CSVInputStream is) throws EventParseException {
		try {
			long time              = is.nextFieldLong();
			long objectTag         = is.nextFieldLong();
			
			if (is.numberOfFieldsLeft()==0) 
				return new EventFree(time, objectTag);
			
		} catch (Exception nfe) { }
		
		throw new EventParseException("format error "+TAG);
	}
}
