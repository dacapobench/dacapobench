package org.dacapo.analysis.util.events;

import org.dacapo.analysis.util.CSVInputStream;
import org.dacapo.analysis.util.CSVOutputStream;
import org.dacapo.analysis.util.CSVInputStream.NoFieldAvailable;
import org.dacapo.analysis.util.CSVInputStream.ParseError;
import org.dacapo.instrument.LogTags;

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
