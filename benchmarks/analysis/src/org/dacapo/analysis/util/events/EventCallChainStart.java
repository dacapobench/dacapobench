package org.dacapo.analysis.util.events;

import org.dacapo.analysis.util.CSVInputStream;
import org.dacapo.analysis.util.CSVOutputStream;
import org.dacapo.analysis.util.CSVInputStream.NoFieldAvailable;
import org.dacapo.analysis.util.CSVInputStream.ParseError;
import org.dacapo.instrument.LogTags;

public class EventCallChainStart extends Event {

	public  static final String TAG = LogTags.LOG_PREFIX_CALL_CHAIN_START;
	
	public EventCallChainStart(long time) {
		super(time);
	}
	
	public String getLogPrefix() {
		return TAG;
	}
	
	protected void writeEvent(CSVOutputStream os) {
		os.write(""+getTime());
	}
	
	EventCallChainStart(CSVInputStream is) throws NoFieldAvailable, ParseError, EventParseException {
		super(is);
		
		if (is.numberOfFieldsLeft()!=0 && this instanceof EventCallChainStart)
			throw new EventParseException("additional field",null);
	}
}
