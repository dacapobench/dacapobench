package org.dacapo.analysis.util.events;

import org.dacapo.instrument.LogTags;
import org.dacapo.util.CSVInputStream;
import org.dacapo.util.CSVOutputStream;
import org.dacapo.util.CSVInputStream.NoFieldAvailable;
import org.dacapo.util.CSVInputStream.ParseError;

public class EventClassPrepare extends Event {

	public  static final String TAG = LogTags.LOG_PREFIX_CLASS_PREPARE;
	
	private long   classTag;
	private String className;
	
	public EventClassPrepare(long time, long classTag, String className) {
		super(time);
		this.classTag  = classTag;
		this.className = className;
	}

	public String getLogPrefix() {
		return TAG;
	}
	
	
	protected void writeEvent(CSVOutputStream os) {
		os.write(""+getTime());
		// os.write(""+classTag);
		os.write(className);
	}

	public long getClassTag() { return classTag; }
	
	public String getClassName() { return className; }

	EventClassPrepare(CSVInputStream is) throws NoFieldAvailable, ParseError, EventParseException {
		super(is);

		// classTag        = is.nextFieldLong();
		className       = is.nextFieldString(); 

		if (is.numberOfFieldsLeft()!=0 && this instanceof EventClassPrepare) 
			throw new EventParseException("additional fields", null);
	}
}
