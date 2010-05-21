package org.dacapo.analysis.util.events;

import org.dacapo.analysis.util.CSVInputStream;
import org.dacapo.instrument.LogTags;

public class EventGC extends Event {

	public  static final String TAG = LogTags.LOG_PREFIX_GC;
		
	public EventGC(long time) {
		super(time);
	}
	
	public String getLogPrefix() {
		return TAG;
	}

	static Event parse(CSVInputStream is) throws EventParseException {
		throw new EventParseException("format error "+TAG);
	}
}
