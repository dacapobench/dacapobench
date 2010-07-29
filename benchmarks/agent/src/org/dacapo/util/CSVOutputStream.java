package org.dacapo.util;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

public class CSVOutputStream {

	private final static char         SEPARATOR       = ',';
	private final static char         DELIMITER       = '"';
	private final static char         NEW_LINE        = '\n';
	private final static char         CARRIAGE_RETURN ='\r';
	
	private PrintStream  ps;
	private OutputStream os;
	private boolean      firstField = true;
	private char         separator;
	private char         delimiter;
	
	public CSVOutputStream(OutputStream os) {
		this(os,DELIMITER,SEPARATOR);
	}
	
	public CSVOutputStream(OutputStream os, char delimiter) {
		this(os,delimiter,SEPARATOR);
	}
	
	public CSVOutputStream(OutputStream os, char delimiter, char separator) {
		this.os = os;
		this.ps = new PrintStream(os);
		this.delimiter = delimiter;
		this.separator = separator;
	}

	public void close() throws IOException {
		ps.close();
		os.close();
	}
	
	public synchronized void write(String field) {
		if (field==null || field.length()==0) {
			writeField("",true);
		} else if (containsCharToBeDelimited(field)) {
			writeField(field,true);
		} else {
			writeField(field,false);
		}
	}
	
	public synchronized void eol() {
		ps.println();
		firstField = true;
	}
	
	private boolean containsCharToBeDelimited(String field) {
		for(char c: field.toCharArray()) {
			if (c==delimiter || c==separator || c==NEW_LINE || c==CARRIAGE_RETURN)
				return true;
		}
		return false;
	}
	
	private void writeField(String field, boolean delimit) {
		if (!firstField)
			ps.print(separator);
		firstField = false;
		if (delimit)
			ps.print(delimiter);
		for(char c: field.toCharArray()) {
			if (c==delimiter)
				ps.print(delimiter);
			ps.print(c);
		}
		if (delimit)
			ps.print(delimiter);
	}

}
