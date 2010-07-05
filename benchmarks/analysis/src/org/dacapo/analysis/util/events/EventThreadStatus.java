package org.dacapo.analysis.util.events;

import org.dacapo.analysis.util.CSVInputStream;
import org.dacapo.analysis.util.CSVOutputStream;
import org.dacapo.analysis.util.CSVInputStream.NoFieldAvailable;
import org.dacapo.analysis.util.CSVInputStream.ParseError;
import org.dacapo.instrument.LogTags;

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
