package org.dacapo.analysis.util.events;

import org.dacapo.instrument.LogTags;
import org.dacapo.util.CSVInputStream;
import org.dacapo.util.CSVOutputStream;
import org.dacapo.util.CSVInputStream.NoFieldAvailable;
import org.dacapo.util.CSVInputStream.ParseError;

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
