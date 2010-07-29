package org.dacapo.analysis.util.events;

import org.dacapo.instrument.LogTags;
import org.dacapo.util.CSVInputStream;
import org.dacapo.util.CSVOutputStream;
import org.dacapo.util.CSVInputStream.NoFieldAvailable;
import org.dacapo.util.CSVInputStream.ParseError;

public class EventStart extends Event {

	public  static final String TAG = LogTags.LOG_PREFIX_START;
	
	public EventStart(long time) {
		super(time);
	}

	public String getLogPrefix() {
		return TAG;
	}

	protected void writeEvent(CSVOutputStream os) {
		os.write(""+getTime());
	}
	
	EventStart(CSVInputStream is) throws NoFieldAvailable, ParseError, EventParseException {
		super(is);

		if (is.numberOfFieldsLeft()!=0 && this instanceof EventStart) 
			throw new EventParseException("additional fields", null);
	}
}
