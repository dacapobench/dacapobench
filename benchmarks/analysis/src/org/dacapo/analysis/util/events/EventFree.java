package org.dacapo.analysis.util.events;

import org.dacapo.analysis.util.CSVInputStream;
import org.dacapo.analysis.util.CSVOutputStream;
import org.dacapo.analysis.util.CSVInputStream.NoFieldAvailable;
import org.dacapo.analysis.util.CSVInputStream.ParseError;
import org.dacapo.instrument.LogTags;

public class EventFree extends Event {

	public  static final String TAG = LogTags.LOG_PREFIX_FREE;
	
	private long objectTag;
	
	public EventFree(long time, long objectTag) {
		super(time);
		this.objectTag = objectTag;
	}
	
	protected void writeEvent(CSVOutputStream os) {
		os.write(""+getTime());
		os.write(""+objectTag);
	}

	public String getLogPrefix() {
		return TAG;
	}

	public long getObjectTag() { return objectTag; }

	EventFree(CSVInputStream is) throws NoFieldAvailable, ParseError, EventParseException {
		super(is);

		objectTag         = is.nextFieldLong();

		if (is.numberOfFieldsLeft()!=0 && this instanceof EventFree) 
			throw new EventParseException("additional fields", null);
	}
}
