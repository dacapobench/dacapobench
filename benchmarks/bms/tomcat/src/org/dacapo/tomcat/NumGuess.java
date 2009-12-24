/*
 * Copyright (c) 2006, 2009 The Australian National University.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License v2.0.
 * You may obtain the license at
 * 
 *    http://www.opensource.org/licenses/apache2.0.php
 */
package org.dacapo.tomcat;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import org.apache.commons.httpclient.methods.GetMethod;

/**
 * Drive the number guessing game - server thinks of a number and the client
 * tries to guess it.
 * 
 * @date $Date: 2009-12-24 11:19:36 +1100 (Thu, 24 Dec 2009) $
 * @id $Id: NumGuess.java 738 2009-12-24 00:19:36Z steveb-oss $
 */
public class NumGuess extends HttpGet {

  NumGuess() {
    super("/examples/jsp/num/numguess.jsp");
  }

  /**
   * @see org.dacapo.tomcat.HttpGet#fetch(org.dacapo.tomcat.Session,
   * java.io.File, boolean)
   */
  @Override
  public boolean fetch(Session session, File logFile, boolean keep) throws IOException {
    Writer output = new FileWriter(logFile);
    try {
      int guess = 64;
      int stride = 32;
      for (int i = 0; i < 10; i++) {
        /*
         * First query uses no parameters - subsequent ones pass the initial
         * guess
         */
        GetMethod get = new GetMethod(formatUrl(session, address + (i == 0 ? "" : "?guess=" + guess)));
        final int iGetResultCode = session.httpClient.executeMethod(get);
        final String responseBody = readStream(get.getResponseBodyAsStream());
        if (keep)
          output.write(responseBody);
        if (iGetResultCode != expectedStatus) {
          System.err.printf("URL %s returned status %d (expected %d)%n", address, iGetResultCode, expectedStatus);
          if (!keep)
            output.write(responseBody);
          return false;
        }

        /*
         * Analyse the response and decide what to do next
         */
        if (responseBody.contains("Congratulations!  You got it.")) {
          return true;
        } else if (responseBody.contains("Welcome to the Number Guess game.")) {
          // First page
        } else if (responseBody.contains("Good guess, but nope.  Try <b>higher</b>.")) {
          guess += stride;
          stride /= 2;
        } else if (responseBody.contains("Good guess, but nope.  Try <b>lower</b>.")) {
          guess -= stride;
          stride /= 2;
        } else {
          if (!keep)
            output.write(responseBody);
          output.write("Unexpected result - quitting\n");
          return false;
        }
      }
      output.write("Too many iterations - exiting\n");
      return false;
    } finally {
      output.close();
    }
  }
}
