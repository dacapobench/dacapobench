package org.dacapo.tomcat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.security.MessageDigest;

import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.dacapo.harness.Digest;

/**
 * Interact with an Http page
 */
public abstract class Page {

  /**
   * The base URL
   */
  protected final String address;

  /**
   * Expected MD5 digest
   */
  protected final String expectedDigest;

  /**
   * Expected Http Status
   */
  protected final int expectedStatus;

  /**
   * Per-session data
   */
  protected final Session session;

  /**
   * An HTTP page, the expected Http status and the md5 digest of the expected result
   * @param session
   * @param address
   * @param status
   * @param digest
   */
  public Page(Session session, String address, int status, String digest) {
    this.address = address;
    this.expectedStatus = status;
    this.expectedDigest = digest;
    this.session = session;
  }

  /**
   * Utility method to read an input stream and return it as a string
   * @param responseStream
   * @return
   * @throws IOException
   */
  protected static String readStream(InputStream responseStream) throws IOException {
    BufferedReader input = new BufferedReader(new InputStreamReader(responseStream));
    StringBuilder reply1 = new StringBuilder(4096);
    for (String line = input.readLine(); line !=null; line = input.readLine()) {
      reply1.append(line);
      reply1.append('\n');
    }
    input.close();
    StringBuilder reply = reply1;

    String replyString = reply.toString();
    return replyString;
  }

  /**
   * Calculate the md5 digest of a given string, and return a string
   * representation thereof
   * 
   * @param str
   * @return
   */
  protected String stringDigest(String str) {
    final MessageDigest md = Digest.create();
    byte[] buf = str.getBytes();
    for (int i=0; i < str.length(); i++) {
      md.update(buf[i]);   
    }
    return Digest.toString(md.digest());  // Must only ever call digest() once!
  }

  /**
   * Sub-classes must override this
   * @param logFile
   * @return
   * @throws IOException
   */
  protected abstract boolean fetch(File logFile, boolean keep) throws IOException;

  public final boolean fetch(File logFile) throws IOException {
    return fetch(logFile,false);
  }
  
  /**
   * Fetch a page from an Http connection.
   * @param method The method to invoke
   * @param logFile Where to write the log (if written)
   * @param keep Write the log on success (always writes on failure)
   * @return Whether the fetch failed or succeeded
   * @throws IOException
   * @throws HttpException
   */
  protected boolean fetch(HttpMethod method, File logFile, boolean keep) throws IOException, HttpException {
    final int iGetResultCode = session.httpClient.executeMethod(method);
    final String strGetResponseBody = readStream(method.getResponseBodyAsStream());
    final String strGetResponseBodyLocalized = strGetResponseBody.replace("\n",System.getProperty("line.separator"));
    if (keep) {
      writeLog(logFile, strGetResponseBodyLocalized);
    }
    
    if (iGetResultCode != expectedStatus) {
      System.err.printf("URL %s returned status %d (expected %d)%n",
          address,iGetResultCode,expectedStatus);
      if (!keep) writeLog(logFile, strGetResponseBodyLocalized);
      return false;
    }

    if (expectedDigest == null) {
      return true;
    }
    
    String digestString = stringDigest(strGetResponseBody);
    boolean digestMatch = digestString.equals(expectedDigest);
    if (!digestMatch) {
      if (!keep) writeLog(logFile, strGetResponseBodyLocalized);
      System.err.printf("URL %s%n" +
          "   expected %s%n" +
          "   found    %s%n" +
          "   response code %d, log file %s%n",
          address,expectedDigest,digestString,iGetResultCode,logFile.getName());
    }
    return digestMatch;
  }

  /**
   * Return the address
   * @return
   */
  public String getAddress() {
    return address;
  }

  /**
   * Write a string to a log file
   * @param logFile
   * @param replyString
   * @throws IOException
   */
  protected void writeLog(File logFile, String replyString) throws IOException {
    Writer output = new FileWriter(logFile);
    output.write(replyString);
    output.close();
  }

  protected String formatUrl() {
    return formatUrl(address);
  }

  protected String formatUrl(String addr) {
    String formattedUrl = String.format("http://localhost:%d%s", Control.port, addr);
    return formattedUrl;
  }

}
