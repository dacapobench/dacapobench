package org.dacapo.analysis.util.events;

import org.dacapo.analysis.util.CSVInputStream;
import org.dacapo.instrument.LogTags;

public class EventVolatile extends Event {

	public  static final String TAG = LogTags.LOG_PREFIX_VOLATILE;
	
	private long   classTag;
	private long   fieldId;
	private String fieldName;
	private String fieldSignature;
	
	public EventVolatile(long time, long classTag, long fieldId, String fieldName, String fieldSignature) {
		super(time);
		this.classTag = classTag;
		this.fieldId = fieldId;
		this.fieldName = fieldName;
		this.fieldSignature = fieldSignature;
	}
	
	public String getLogPrefix() {
		return TAG;
	}
	
	static Event parse(CSVInputStream is) throws EventParseException {
		try {
			long time              = is.nextFieldLong();

			long   classTag        = is.nextFieldLong();
			long   fieldId         = is.nextFieldPointer();
			String fieldName       = is.nextFieldString();
			String fieldSignature  = is.nextFieldString();
			
			if (is.numberOfFieldsLeft()==0) 
				return new EventVolatile(time, classTag, fieldId, fieldName, fieldSignature);
		} catch (Exception nfe) { }
		
		throw new EventParseException("format error "+TAG);
	}
}
