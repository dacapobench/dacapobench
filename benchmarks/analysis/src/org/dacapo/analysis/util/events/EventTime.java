package org.dacapo.analysis.util.events;

import org.dacapo.analysis.util.CSVInputStream;
import org.dacapo.analysis.util.CSVOutputStream;
import org.dacapo.analysis.util.CSVInputStream.NoFieldAvailable;
import org.dacapo.analysis.util.CSVInputStream.ParseError;
import org.dacapo.instrument.LogTags;

public class EventTime extends Event {

	public  static final String TAG = LogTags.LOG_PREFIX_TIME;
	
	public EventTime(long time) {
		super(time);
	}
	
	public String getLogPrefix() {
		return TAG;
	}

	protected void writeEvent(CSVOutputStream os) {
		os.write(""+getTime());
	}
	
	EventTime(CSVInputStream is) throws NoFieldAvailable, ParseError, EventParseException {
		super(is);
		
		if (is.numberOfFieldsLeft()!=0 && this instanceof EventTime) 
			throw new EventParseException("additional fields", null);
	}
}
