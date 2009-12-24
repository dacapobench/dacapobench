/*
 * Copyright (c) 2006, 2009 The Australian National University.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License v2.0.
 * You may obtain the license at
 * 
 *    http://www.opensource.org/licenses/apache2.0.php
 */
package org.dacapo.tomcat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.security.MessageDigest;

import org.apache.commons.httpclient.HttpMethod;
import org.dacapo.harness.Digest;

/**
 * Interact with an Http page
 * 
 * @date $Date: 2009-12-24 11:19:36 +1100 (Thu, 24 Dec 2009) $
 * @id $Id: Page.java 738 2009-12-24 00:19:36Z steveb-oss $
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
   * An HTTP page, the expected Http status and the md5 digest of the expected
   * result
   * @param address The URL (host-relative)
   * @param status Expected status code
   * @param digest Expected page digest (null for none)
   */
  public Page(String address, int status, String digest) {
    this.address = address;
    this.expectedStatus = status;
    this.expectedDigest = digest;
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
    for (String line = input.readLine(); line != null; line = input.readLine()) {
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
    for (int i = 0; i < str.length(); i++) {
      md.update(buf[i]);
    }
    return Digest.toString(md.digest()); // Must only ever call digest() once!
  }

  /**
   * Sub-classes must override this
   * @param logFile
   * @return
   * @throws IOException
   */
  protected abstract boolean fetch(Session session, File logFile, boolean keep) throws IOException;

  /**
   * Fetch a page from an Http connection, without keeping the log file.
   * @param session The HTTP session
   * @param logFile Destination for the log file
   * @return Whether the fetch failed or succeeded
   * @throws IOException A network or disk I/O error
   */
  public final boolean fetch(Session session, File logFile) throws IOException {
    return fetch(session, logFile, false);
  }

  /**
   * Fetch a page from an Http connection.
   * @param method The method to invoke
   * @param logFile Where to write the log (if written)
   * @param keep Write the log on success (always writes on failure)
   * @return Whether the fetch failed or succeeded
   * @throws IOException A network or disk I/O error
   */
  protected final boolean fetch(Session session, HttpMethod method, File logFile, boolean keep) throws IOException {
    final int iGetResultCode = session.httpClient.executeMethod(method);
    final String strGetResponseBody = readStream(method.getResponseBodyAsStream());
    final String strGetResponseBodyLocalized = strGetResponseBody.replace("\n", System.getProperty("line.separator"));
    if (keep) {
      writeLog(logFile, strGetResponseBodyLocalized);
    }

    if (iGetResultCode != expectedStatus) {
      System.err.printf("URL %s returned status %d (expected %d)%n", address, iGetResultCode, expectedStatus);
      if (!keep)
        writeLog(logFile, strGetResponseBodyLocalized);
      return false;
    }

    if (expectedDigest == null) {
      return true;
    }

    String digestString = stringDigest(strGetResponseBody);
    boolean digestMatch = digestString.equals(expectedDigest);
    if (!digestMatch) {
      if (!keep)
        writeLog(logFile, strGetResponseBodyLocalized);
      System.err.printf("URL %s%n" + "   expected %s%n" + "   found    %s%n" + "   response code %d, log file %s%n", address, expectedDigest, digestString,
          iGetResultCode, logFile.getName());
    }
    return digestMatch;
  }

  /**
   * Return the address
   * @return The page-relative address
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

  protected String formatUrl(Session session) {
    return formatUrl(session, address);
  }

  static String formatUrl(Session session, String addr) {
    String formattedUrl = String.format("http://localhost:%d%s", session.getPort(), addr);
    return formattedUrl;
  }

}
