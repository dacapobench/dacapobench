package org.dacapo.analysis.util.events;

import org.dacapo.analysis.util.CSVInputStream;
import org.dacapo.analysis.util.CSVOutputStream;
import org.dacapo.analysis.util.CSVInputStream.NoFieldAvailable;
import org.dacapo.analysis.util.CSVInputStream.ParseError;
import org.dacapo.instrument.LogTags;

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
