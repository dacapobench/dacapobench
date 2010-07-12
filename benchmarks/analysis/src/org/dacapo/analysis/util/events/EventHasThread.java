package org.dacapo.analysis.util.events;

public interface EventHasThread {

	public long getThreadTag();
	public void setThreadTag(long threadTag);
	
	public long getThreadClassTag();
	public void setThreadClassTag(long threadClassTag);
	
	public String getThreadClass();
	public void   setThreadClass(String threadClass);
	
	public String getThreadName();
	public void   setThreadName(String threadName);
	
}
