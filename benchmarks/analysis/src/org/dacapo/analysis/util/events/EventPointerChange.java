package org.dacapo.analysis.util.events;

import org.dacapo.instrument.LogTags;
import org.dacapo.util.CSVInputStream;
import org.dacapo.util.CSVOutputStream;
import org.dacapo.util.CSVInputStream.NoFieldAvailable;
import org.dacapo.util.CSVInputStream.ParseError;

public class EventPointerChange extends Event implements EventHasThread {
	public  static final String TAG = LogTags.LOG_PREFIX_POINTER;
	
	private long   threadTag;
	private long   threadClassTag;
	private String threadClass;
	private String threadName;
	private Long   objectTag;
	private Long   objectClassTag;
	private String objectClassName;
	private Long   beforeTag;
	private Long   beforeClassTag;
	private String beforeClassName;
	private Long   afterTag;
	private Long   afterClassTag;
	private String afterClassName;
	
	public EventPointerChange(long time,
			Long threadTag, Long   threadClassTag, String threadClassName, String threadName,
			Long objectTag, Long   objectClassTag, String objectClassName,
			Long beforeTag, Long   beforeClassTag, String beforeClassName,
			Long afterTag,  Long   afterClassTag,  String afterClassName) {
		super(time);
		this.threadTag       = threadTag;
		this.threadClassTag  = threadClassTag;
		this.threadClass     = threadClassName;
		this.threadName      = threadName;
		this.objectTag       = objectTag;
		this.objectClassTag  = objectClassTag;
		this.objectClassName = objectClassName;
		this.beforeTag       = beforeTag;
		this.beforeClassTag  = beforeClassTag;
		this.beforeClassName = beforeClassName;
		this.afterTag        = afterTag;
		this.afterClassTag   = afterClassTag;
		this.afterClassName  = afterClassName;
	}

	public String getLogPrefix() {
		return TAG;
	}
	
	protected void writeEvent(CSVOutputStream os) {
		os.write(""+getTime());
		
		EventThread.write(os, this);

		os.write(""+objectTag);
		os.write(""+objectClassTag);
		os.write(objectClassName);
		os.write(""+beforeTag);
		os.write(""+beforeClassTag);
		os.write(beforeClassName);
		os.write(""+afterTag);
		os.write(""+afterClassTag);
		os.write(afterClassName);
	}
	
	public long getThreadTag() { return threadTag; }
	public void setThreadTag(long threadTag) { this.threadTag = threadTag; }
	
	public long getThreadClassTag() { return threadClassTag; }
	public void setThreadClassTag(long threadClassTag) { this.threadClassTag = threadClassTag; }
	
	public String getThreadClass() { return threadClass; }
	public void   setThreadClass(String threadClass) { this.threadClass = threadClass; }
	
	public String getThreadName() { return threadName; }
	public void   setThreadName(String threadName) { this.threadName = threadName; }
	
	public Long getObjectTag() { return objectTag; }
	public Long getObjectClassTag() { return objectClassTag; }
	public String getObjectClassName() { return objectClassName; }
	
	public Long getBeforeTag() { return beforeTag; }
	public Long getBeforeClassTag() { return beforeClassTag; }
	public String getBeforeClassName() { return beforeClassName; }
	
	public Long getAfterTag() { return afterTag; }
	public Long getAfterClassTag() { return afterClassTag; }
	public String getAfterClassName() { return afterClassName; }
	
	EventPointerChange(CSVInputStream is) throws NoFieldAvailable, ParseError, EventParseException {
		super(is);
		
		EventThread.read(is, this);
		
		this.objectTag         = is.nextFieldLong();
		this.objectClassTag    = is.nextFieldLong();
		this.objectClassName   = is.nextFieldString();

		this.beforeTag         = is.nextFieldLong();
		this.beforeClassTag    = is.nextFieldLong();
		this.beforeClassName   = is.nextFieldString();

		this.afterTag          = is.nextFieldLong();
		this.afterClassTag     = is.nextFieldLong();
		this.afterClassName    = is.nextFieldString();

		if (is.numberOfFieldsLeft()!=0 && this instanceof EventPointerChange) 
			throw new EventParseException("additional fields", null);
	}
}
