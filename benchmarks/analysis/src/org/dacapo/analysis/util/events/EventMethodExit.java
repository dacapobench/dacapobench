package org.dacapo.analysis.util.events;

import org.dacapo.instrument.LogTags;
import org.dacapo.util.CSVInputStream;
import org.dacapo.util.CSVOutputStream;
import org.dacapo.util.CSVInputStream.NoFieldAvailable;
import org.dacapo.util.CSVInputStream.ParseError;

public class EventMethodExit extends Event {

	public  static final String TAG = LogTags.LOG_PREFIX_METHOD_EXIT;
	
	public EventMethodExit(long time) {
		super(time);
	}
	
	public String getLogPrefix() { return TAG; }
	
	protected void writeEvent(CSVOutputStream os) {
		os.write(""+getTime());
	}
	
	EventMethodExit(CSVInputStream is) throws EventParseException, NoFieldAvailable, ParseError {
		super(is);
		
		// EventThread.read(is, this);

		// this.methodId          = is.nextFieldPointer();
		
		if (is.numberOfFieldsLeft()!=0 && this instanceof EventMethodExit) 
			throw new EventParseException("additional fields", null);
	}
}
