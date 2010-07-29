package org.dacapo.analysis.util.events;

import org.dacapo.util.CSVInputStream;
import org.dacapo.util.CSVOutputStream;
import org.dacapo.util.CSVInputStream.NoFieldAvailable;
import org.dacapo.util.CSVInputStream.ParseError;

public final class EventThread {

	public static void read(CSVInputStream is, EventHasThread evt) throws EventParseException, NoFieldAvailable, ParseError {
		long   threadTag       = is.nextFieldLong();
		String threadName      = is.nextFieldString();
		long   threadClassTag  = is.nextFieldLong();
		String threadClass     = is.nextFieldString();

		evt.setThreadTag(threadTag);
		evt.setThreadClassTag(threadClassTag);
		evt.setThreadClass(threadClass);
		evt.setThreadName(threadName);
	}
	
	public static void write(CSVOutputStream os, EventHasThread evt) {
		os.write(""+evt.getThreadTag());
		os.write(""+evt.getThreadClassTag());
		os.write(evt.getThreadClass());
		os.write(evt.getThreadName());
	}
	
}
