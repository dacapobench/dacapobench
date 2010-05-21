package org.dacapo.analysis.util.events;

import org.dacapo.analysis.util.CSVInputStream;
import org.dacapo.instrument.LogTags;

public class EventMethodPrepare extends Event {

	public  static final String TAG = LogTags.LOG_PREFIX_METHOD_PREPARE;
	
	private long methodId;
	private long classTag;
	private String methodName;
	private String methodSignature;
	
	public EventMethodPrepare(long time, long methodId, long classTag, String methodName, String methodSignature) {
		super(time);
		this.methodId = methodId;
		this.classTag = classTag;
		this.methodName = methodName;
		this.methodSignature = methodSignature;
	}

	public String getLogPrefix() {
		return TAG;
	}
	
	static Event parse(CSVInputStream is) throws EventParseException {
		try {
			long time              = is.nextFieldLong();

			long methodId          = is.nextFieldPointer();

			long classTag          = is.nextFieldLong();
			String methodName      = is.nextFieldString();
			String methodSignature = is.nextFieldString();

			if (is.numberOfFieldsLeft()==0) 
				return new EventMethodPrepare(time, methodId, classTag, methodName, methodSignature);
			
		} catch (Exception nfe) { }
		
		throw new EventParseException("format error "+TAG);
	}

}
