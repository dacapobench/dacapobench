package org.dacapo.analysis.util.events;

import org.dacapo.instrument.LogTags;
import org.dacapo.util.CSVInputStream;
import org.dacapo.util.CSVOutputStream;
import org.dacapo.util.CSVInputStream.NoFieldAvailable;
import org.dacapo.util.CSVInputStream.ParseError;

public class EventThreadStatus extends Event {

	public  static final String TAG = LogTags.LOG_PREFIX_THREAD_STATUS;
	
	private long threadTag;
	
	public EventThreadStatus(long time, long threadTag) {
		super(time);
		this.threadTag = threadTag;
	}
	
	public String getLogPrefix() { return TAG; }
	
	public long getThreadTag() { return threadTag; }
	
	public String toString() {
		return super.toString()+":"+getThreadTag();
	}
	
	protected void writeEvent(CSVOutputStream os) {
		os.write(""+getTime());
		os.write(""+threadTag);
	}
	
	EventThreadStatus(CSVInputStream is) throws NoFieldAvailable, ParseError, EventParseException {
		super(is);

		threadTag = is.nextFieldLong();

		if (is.numberOfFieldsLeft()!=0 && this instanceof EventThreadStatus) 
			throw new EventParseException("additional fields",null);
	}
}
