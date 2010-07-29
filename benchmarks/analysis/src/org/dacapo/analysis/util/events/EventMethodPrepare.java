package org.dacapo.analysis.util.events;

import org.dacapo.instrument.LogTags;
import org.dacapo.util.CSVInputStream;
import org.dacapo.util.CSVOutputStream;
import org.dacapo.util.CSVInputStream.NoFieldAvailable;
import org.dacapo.util.CSVInputStream.ParseError;

public class EventMethodPrepare extends Event {

	public  static final String TAG = LogTags.LOG_PREFIX_METHOD_PREPARE;
	
	private long methodId;
	private long classTag;
	private String classSignature;
	private String methodName;
	private String methodSignature;
	
	public EventMethodPrepare(long time, long methodId, long classTag, String classSignature, String methodName, String methodSignature) {
		super(time);
		this.methodId = methodId;
		this.classTag = classTag;
		this.classSignature = classSignature;
		this.methodName = methodName;
		this.methodSignature = methodSignature;
	}

	public String getLogPrefix() {
		return TAG;
	}
	
	protected void writeEvent(CSVOutputStream os) {
		os.write(""+getTime());
		os.write(classSignature);
		os.write(""+methodId);
		os.write(methodName);
		os.write(methodSignature);
	}
	
	EventMethodPrepare(CSVInputStream is) throws EventParseException, NoFieldAvailable, ParseError {
		super(is);
		
		// classTag        = is.nextFieldLong();
		classSignature  = is.nextFieldString();

		methodId        = is.nextFieldPointer();

		methodName      = is.nextFieldString();
		methodSignature = is.nextFieldString();
		
		if (is.numberOfFieldsLeft()!=0 && this instanceof EventMethodPrepare) 
			throw new EventParseException("additional fields", null);
	}
}
