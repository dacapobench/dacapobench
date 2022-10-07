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
import java.util.Arrays;
import java.util.List;

import org.dacapo.harness.LatencyReporter;

/**
 * A client of the tomcat benchmark.
 * 
 * Each client iterates through a list of queries for a given number of
 * iterations, using a single session context.
 * 
 * date:  $Date: 2009-12-24 11:19:36 +1100 (Thu, 24 Dec 2009) $
 * id: $Id: Client.java 738 2009-12-24 00:19:36Z steveb-oss $
 */
public class Client implements Runnable {

  private final File logDir;
  private final int ordinal;
  private final int pageCount;
  private final boolean verbose;
  private final int port;
  private final int fivePercent;

  /**
   * The pages to iterate through
   */
  public static final List<Page> pages = Arrays.asList(
      new HttpGet("/examples/jsp/jsp2/el/basic-arithmetic.jsp","dc3db1fb460a427ad6aef0b71d42319213a81f67"),
      new HttpGet("/examples/jsp/jsp2/el/basic-comparisons.jsp","7ea08cc03fe9ff5d3cdd7cec7ae5dc1badc2da60"),
      new HttpGet("/examples/jsp/jsp2/el/implicit-objects.jsp?foo=bar","3f8a13e109a21ebf947c8cba40c077ba7c90e0e5"),
      new HttpGet("/examples/jsp/jsp2/el/functions.jsp?foo=JSP+2.0","880dab5554db41d30c52c86ad016b3e7b4fc25ff"),
      new HttpGet("/examples/jsp/jsp2/simpletag/hello.jsp","fa2cc0fd8bf403beb0e2b55b282b2a9c1e1d8f01"),
      new HttpGet("/examples/jsp/jsp2/simpletag/repeat.jsp","5f7c6a02f0307f082015d35ed03cf6ff55bc88e3"),
      new HttpGet("/examples/jsp/jsp2/simpletag/book.jsp","3b97d860d81304cd02b530b0da073861c7e97f10"),
      new HttpGet("/examples/jsp/jsp2/tagfiles/hello.jsp","997ebe20a346b21ce59550d05220ef90dd7d892e"),
      new HttpGet("/examples/jsp/jsp2/tagfiles/panel.jsp","cd34787cd5ee0f8c526a65f51851b6441ab8b3d3"),
      new HttpGet("/examples/jsp/jsp2/tagfiles/products.jsp","fb9cf67a579e2af4b6b10065102936f64ce86859"),

      // Prints the date - don't validate
      new HttpGet("/examples/jsp/jsp2/jspx/basic.jspx",200),
      new HttpGet("/examples/jsp/jsp2/jspx/svgexample.html",200,"c32e91590a3e031b94cd992fd6fd458960ffb191"),
      new HttpGet("/examples/jsp/jsp2/jspx/textRotate.jspx?name=JSPX",200,"62d28df98503af07933c280c991d2742edc5f9f9"),
      new HttpGet("/examples/jsp/jsp2/jspattribute/jspattribute.jsp","b04951dbbbaef1976f0d0bb31bee982c03a2c212"),

      // Shuffle is by definition dynamic
      new HttpGet("/examples/jsp/jsp2/jspattribute/shuffle.jsp",200),
      new HttpGet("/examples/jsp/jsp2/misc/dynamicattrs.jsp","84097bf8eb796a6c6b4c7c0f9ba755955ee1d459"),
      new HttpGet("/examples/jsp/jsp2/misc/config.jsp","9c09e106a8d1d737c4ebdcebbecc0b0e55440238"),

      // Number guessing game
      new NumGuess(),

      // Shows the current time, so can't be digested
      new HttpGet("/examples/jsp/dates/date.jsp"),

      // Can return system-specific data
      new HttpGet("/examples/jsp/snp/snoop.jsp",200),

      new HttpGet("/examples/jsp/error/error.html",200,"7a4eee413a6d4ebc66baca65c1fcf4c2dd1e9904"),
      new HttpGet("/examples/jsp/error/err.jsp?name=audi&submit=Submit",500,"4b5b51a41a2720387c089e6f830684578bc40393"),
      new HttpGet("/examples/jsp/error/err.jsp?name=integra&submit=Submit",200,"520431c6f0c3a42c33eff68cf4cf89ceda9c074c"),

      /*
       * The following group of methods form a  adding items to a shopping cart
       * and then removing them.  Because each client should have a single persistent
       * session across several iterations through this list, we should leave the session
       * as we find it.
       */
      new HttpGet("/examples/jsp/sessions/carts.html",200,"91ec746482c18aa26f6b155ceedc584c917c6f01"),
      new HttpPost("/examples/jsp/sessions/carts.jsp?item=NIN+CD&submit=add",200,"a21a3e287b1237e7007e6e53d9ec0093b23f1780"),
      new HttpPost("/examples/jsp/sessions/carts.jsp?item=JSP+Book&submit=add",200,"bad7c7ec9be65562d3f07c395deee01951bba130"),
      new HttpPost("/examples/jsp/sessions/carts.jsp?item=Love+life&submit=add",200,"9750717cae0d8efd6367867e9d5aa37ddfed0d14"),
      new HttpPost("/examples/jsp/sessions/carts.jsp?item=NIN+CD&submit=remove",200,"487e47c9090a39298dd1bdd40b8f2cfc1e40890c"),
      new HttpPost("/examples/jsp/sessions/carts.jsp?item=JSP+Book&submit=remove",200,"8ab2bc0c1a2470b5c4a7fb29e4312117e1336e7b"),
      new HttpPost("/examples/jsp/sessions/carts.jsp?item=Love+life&submit=remove",200,"572d089d7d6d2e209e4f82f0297737cefd88ea13"),

      /*
       * The validation here is exhibiting weirdness - TODO fix before release
       *
       * Looks so far like a bug in tomcat :-/
       */
      new HttpGet("/examples/jsp/checkbox/check.html",200,"7a8cff196197a6ed7562a45c7844ac6bf99b02a7"),
      new HttpGet("/examples/jsp/colors/colors.html","3f78993d965d6ce5c46663a44771f97d3eeea16a"),
      new HttpGet("/examples/jsp/cal/login.html","0f7a38c4bcc6b1ff4de306890fc4b3cac98214ed"),

      // Shows the current time, so can't be digested
      new HttpGet("/examples/jsp/include/include.jsp",200),

      // Forwards to one of two files based on VM usage, so can't be digested
      new HttpGet("/examples/jsp/forward/forward.jsp",200),

      // Just shows the "plugin not accepted' response
      new HttpGet("/examples/jsp/jsptoserv/jsptoservlet.jsp",200,"e3546b90c41561db4f0fca4ca7115112a11d0506"),

      // Writes the date
      new HttpGet("/examples/jsp/xml/xml.jsp",200),
      new HttpGet("/examples/jsp/tagplugin/if.jsp",200,"c35432566834ada8b9362af6307093ae4c2dd185"),
      new HttpGet("/examples/jsp/tagplugin/foreach.jsp",200,"7380ee5da006fe29df4baca0e38b6f754cc5303f"),
      new HttpGet("/examples/jsp/tagplugin/choose.jsp",200,"e4a24b5eb600085105a532105f104cf0b8cad48d")
  );
  public static final int BATCH_SIZE = pages.size();

  /**
   * A benchmark client, one per client thread.
   * @param logDir Directory, destination for log files
   * @param ordinal A unique identifier for the client thread
   * @param pageCount # loops to perform
   * @param verbose If true, log verbosely (invalidates benchmark)
   * @param port TCP port for the tomcat server
   * @throws IOException From creation of the client log file
   */
  public Client(File logDir, int ordinal, int pageCount, boolean verbose, int port) throws IOException {
    this.logDir = logDir;
    this.ordinal = ordinal;
    this.pageCount = pageCount;
    this.verbose = verbose;
    this.port = port;
    fivePercent = BATCH_SIZE*(pageCount/20);
  }

  /**
   * @see java.lang.Runnable#run()
   */
  public void run() {
    final Session session = Session.create(port);
    final int totalRequests = pageCount * BATCH_SIZE;
    try {
      int idx = 0;
      while ((idx = LatencyReporter.start(ordinal)) < totalRequests) {
        Page page = pages.get(idx % BATCH_SIZE);
        page.fetch(session, null, verbose);
        LatencyReporter.end(ordinal);
        int done = idx + 1;
        if (fivePercent > 0 && (done % fivePercent == 0)) {
          int percentage = 5 * (done / fivePercent);
          System.err.print("Completing requests: "+percentage+"%\r");
          if (done == totalRequests)
            System.err.println();
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      session.shutdown();
    }
  }
}
