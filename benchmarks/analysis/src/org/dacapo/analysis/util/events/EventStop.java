package org.dacapo.analysis.util.events;

import org.dacapo.analysis.util.CSVInputStream;
import org.dacapo.analysis.util.CSVInputStream.NoFieldAvailable;
import org.dacapo.analysis.util.CSVInputStream.ParseError;
import org.dacapo.instrument.LogTags;

public class EventStop extends Event {

	public  static final String TAG = LogTags.LOG_PREFIX_STOP;
	
	public EventStop(long time) {
		super(time);
	}

	public String getLogPrefix() {
		return TAG;
	}

	static Event parse(CSVInputStream is) throws EventParseException {
		try {
			long time = is.nextFieldLong();
			
			if (is.numberOfFieldsLeft()==0) 
				return new EventStop(time);
			
		} catch (Exception nfe) { }
		
		throw new EventParseException("format error "+TAG);
	}
}
