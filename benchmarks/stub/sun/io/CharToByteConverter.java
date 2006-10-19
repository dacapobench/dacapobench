/*
 * (C) Copyright Department of Computer Science,
 * Australian National University. 2006
 */

package sun.io;

import java.io.UnsupportedEncodingException;

/**
 * This class is not provided by runtimes that aren't based on the sun libraries
 * This class does the minimum required for the xalan benchmark to run.
 * 
 * @author Robin Garner
 * @author Eric Bodden
 * @date $Date:$
 * @id $Id:$
 *
 */
public class CharToByteConverter {
  private static final CharToByteConverter instance = new CharToByteConverter();
  public static CharToByteConverter getConverter(String converter) throws UnsupportedEncodingException {
    return instance;
  }
  public boolean canConvert(char c) {
    return true;
  }
  public int getBadInputLength() {
    return 256; //some made-up value
  }
  public int getMaxBytesPerChar() {
    return 256; //some made-up value
  }
  public int nextByteIndex() {
    return 256; //some made-up value
  }
  public int nextCharIndex() {
    return 256; //some made-up value
  }
  public void reset() {}
  public void setSubstitutionMode(boolean b) {}
  public void setSubstitutionBytes(byte[] b) throws IllegalArgumentException {} 
  public int flush(byte[] a,int b,int c) throws MalformedInputException, ConversionBufferFullException {
    return 256; //some made-up value
  }
  public int flushAny(byte[] a,int b,int c) throws ConversionBufferFullException {
    return 256; //some made-up value
  }
  public byte[] convertAll(char[] a) throws MalformedInputException {
    return new byte[0];
  }
  public int convert(byte[] a,int b,int c) throws MalformedInputException, UnknownCharacterException, ConversionBufferFullException {
    return 256; //some made-up value
  }
  public int convertAny(char[] a, int b, int c, byte[] d, int e, int f) throws ConversionBufferFullException {
    return 256; //some made-up value
  }
  public String getCharacterEncoding() {
    return "";
  }
  public String toString() {
    return "";
  }
  public static CharToByteConverter getDefault() {
    return instance;
  }
}