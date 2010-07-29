package org.dacapo.analysis.util.events;

import org.dacapo.instrument.LogTags;
import org.dacapo.util.CSVInputStream;
import org.dacapo.util.CSVOutputStream;
import org.dacapo.util.CSVInputStream.NoFieldAvailable;
import org.dacapo.util.CSVInputStream.ParseError;

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
	
	protected void writeEvent(CSVOutputStream os) {
		os.write(""+getTime());
		os.write(""+classTag);
		os.write(""+fieldId);
		os.write(fieldName);
		os.write(fieldSignature);
	}
	
	EventVolatile(CSVInputStream is) throws NoFieldAvailable, ParseError, EventParseException {
		super(is);
		
		classTag        = is.nextFieldLong();
		fieldId         = is.nextFieldPointer();
		fieldName       = is.nextFieldString();
		fieldSignature  = is.nextFieldString();

		if (is.numberOfFieldsLeft()!=0 && this instanceof EventVolatile) 
			throw new EventParseException("additional fields", null);
	}
}
