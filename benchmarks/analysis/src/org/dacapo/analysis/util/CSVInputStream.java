package org.dacapo.analysis.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.LinkedList;
import java.util.List;

public class CSVInputStream {
	
	private final static char         SEPARATOR       = ',';
	private final static char         DELIMITER       = '"';
	private final static char         NEW_LINE        = '\n';
	private final static char         CARRIAGE_RETURN = '\r';
	
	private final static int          EOF             = -1;
	
	private InputStream        is;
	
	private char               separator;
	private char               delimiter;
	private boolean            eof;
	private boolean            eol;
	
	private int                parseRow  = 1;
	private int                parseChar;
	
	private LinkedList<String> currentRow;
	
	public CSVInputStream(InputStream is) {
		this(is,DELIMITER,SEPARATOR);
	}
	
	public CSVInputStream(InputStream is, char delimiter) {
		this(is,delimiter,SEPARATOR);
	}
	
	public CSVInputStream(InputStream is, char delimiter, char separator) {
		this.is = is;
		this.delimiter = delimiter;
		this.separator = separator;
		this.currentRow = new LinkedList<String>();
	}
	
	public synchronized String nextFieldString() throws NoFieldAvailable {
		if (currentRow.isEmpty())
			throw new NoFieldAvailable("no fields left");
		return currentRow.pop();
	}
	
	public boolean nextFieldBoolean() throws NoFieldAvailable {
		String field = nextFieldString();
		
		return "true".compareToIgnoreCase(field)==0 || "1".compareTo(field)==0;
	}
	
	public int nextFieldInt() throws NoFieldAvailable, ParseError {
		String field = nextFieldString();
		try {
			return Integer.parseInt(field);
		} catch (Exception e) {
			throw new ParseError("unable to parse field as int");
		}
	}
	
	public long nextFieldLong() throws NoFieldAvailable, ParseError {
		String field = nextFieldString();
		try {
			return Long.parseLong(field);
		} catch (Exception e) {
			throw new ParseError("unable to parse field as long");
		}
	}
	
	public boolean nextRow() throws CSVException {
		if (eof) return false;
		
		boolean found = true;

		eol = false;
		currentRow.clear();
		parseChar = 0;
		
		while (found && !eol) {
			String field = readField();
			
			found = field != null;
			
			if (found) currentRow.addLast(field);
		}
		
		boolean foundRow = !currentRow.isEmpty() || !eof;
		
		if (foundRow) parseRow++;
		
		return foundRow;
	}
	
	public int numberOfFieldsLeft() {
		return currentRow.size();
	}
	
	private String readField() throws CSVException {
		StringBuffer b = new StringBuffer(16);
		int c;
		boolean endOfField  = false;
		boolean fieldDone   = false;
		boolean isDelimited = false;
		boolean endDelimited = false;

		try {
			c = readChar();
			parseChar ++;
			
			while (c != EOF && !eol && !endOfField) {
				char ch = (char)c;
				
				if (isDelimited && !endDelimited) {
					if (ch==delimiter) {
						char tmp = (char)peekChar();
						if (tmp==delimiter) {
							readChar();
							parseChar ++;
							b.append(ch);
						} else {
							endDelimited = true;
							fieldDone    = true;
						}
					} else {
						b.append(ch);
					}
					c = readChar();
					parseChar ++;
				} else if (ch == NEW_LINE) {
					char tmp = (char)peekChar();
					if (tmp == CARRIAGE_RETURN) readChar();
					eol = true;
					endOfField = true;
					fieldDone  = true;
				} else if (ch == CARRIAGE_RETURN) {
					char tmp = (char)peekChar();
					if (tmp == NEW_LINE) readChar();
					eol = true;
					endOfField = true;
					fieldDone  = true;
				} else if (ch == separator) {
					if (b.length()!=0) fieldDone = true;
					endOfField = true;
				} else if (ch == delimiter) {
					if (b.length()==0 && !isDelimited)
						isDelimited = true;
					else
						throw new ParseError("delimiter found in non-delimited field Row#"+parseRow+" Col#"+parseChar);
					c = readChar();
					parseChar ++;
				} else if (fieldDone) {
					throw new ParseError("white space in non-delimited field Row#"+parseRow+" Col#"+parseChar);
				} else {
					b.append(ch);
					c = readChar();
					parseChar ++;
				}
			}
			eof = c == EOF;
			if (eof && !isDelimited && !endDelimited && b.length()!=0)
				fieldDone = endOfField = true;
		} catch (IOException ioe) {
			throw new ParseError(ioe.getMessage());
		}

		if (isDelimited && endDelimited)
			return b.toString();
		else if (isDelimited || endDelimited) {
			throw new ParseError("unbalanced delimiters Row#"+parseRow+" Col#"+parseChar);
		} else if ((!eof || b.length()!=0) && (!fieldDone || !endOfField)) {
			throw new ParseError("field incomplete Row#"+parseRow+" Col#"+parseChar);
		}
		
		if (isDelimited || b.length()!=0)
			return b.toString();
		else
			return null;
	}
	
	private int peekChar() {
		try {
			setMark();
			int c = readChar();
			reset();
			return c;
		} catch (IOException ioe) {
			return EOF;
		}
	}

	private LinkedList<Integer> inputBuffer = new LinkedList<Integer>();
	private boolean mark = false;
	private boolean inputEOF = false;
	
	private void setMark() throws IOException {
		assert !mark;
		mark = true;
	}
	
	private void reset() throws IOException {
		assert mark;
		mark = false;
	}
	
	private int readChar() throws IOException {
		if (mark) {
			int c = is.read();
			if (c == EOF) 
				inputEOF = true;
			else
				inputBuffer.add(c);
			return c;
		} else {
			if (inputBuffer.isEmpty()) {
				if (inputEOF)
					return EOF;
				int c = is.read();
				if (c == EOF)
					inputEOF = true;
				return c;
			} else {
				return inputBuffer.pop();
			}
		}
	}
	
	public static class CSVException extends Exception {
		public CSVException(String message) {
			super(message);
		}
	};
	
	public static class NoFieldAvailable extends CSVException {
		public NoFieldAvailable(String message) {
			super(message);
		}
	};
	
	public static class ParseError extends CSVException {
		public ParseError(String message) {
			super(message);
		}
	};

	public static void main(String[] args) throws Exception {
		CSVInputStream is = new CSVInputStream(new FileInputStream(new File(args[0])));
		
		int rowCount = 0;
		while (is.nextRow()) {
			System.out.println("ROW #"+ ++rowCount);
			int fieldCount = 0;
			while (is.numberOfFieldsLeft()!=0) {
				System.out.println("  Field #"+ ++fieldCount + is.nextFieldString());
			}
		}
	}
}
