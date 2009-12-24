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
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

/**
 * A client of the tomcat benchmark.
 * 
 * Each client iterates through a list of queries for a given number of
 * iterations, using a single session context.
 * 
 * @date $Date: 2009-12-24 11:19:36 +1100 (Thu, 24 Dec 2009) $
 * @id $Id: Client.java 738 2009-12-24 00:19:36Z steveb-oss $
 */
public class Client implements Runnable {

  private final File logDir;
  private final int ordinal;
  private final int pageCount;
  private final boolean verbose;
  private final PrintWriter log;
  private final int port;

  /**
   * The pages to iterate through
   */
  private final List<Page> pages = Arrays.asList(
      new HttpGet("/examples/jsp/jsp2/el/basic-arithmetic.jsp","396eaab04e83090b4d4b259eeca17a5990b7fd73"),
      new HttpGet("/examples/jsp/jsp2/el/basic-comparisons.jsp","2b8b76a06d5b9d3be2ed4c80efcab2adaaad880c"),
      new HttpGet("/examples/jsp/jsp2/el/implicit-objects.jsp?foo=bar","e799ace810ab35a0b56627ddd276e4ad35dbaff4"),
      new HttpGet("/examples/jsp/jsp2/el/functions.jsp?foo=JSP+2.0","c30b0bc98277ad4c2ce48388b3017873f9ca6c9b"),
      new HttpGet("/examples/jsp/jsp2/simpletag/hello.jsp","b741884a658f677c7223296c6138089043030024"),
      new HttpGet("/examples/jsp/jsp2/simpletag/repeat.jsp","a171bec81dbabd3ad22f4694e54c79d82c5286b9"),
      new HttpGet("/examples/jsp/jsp2/simpletag/book.jsp","56044c60aa6744e100033ac7ee1d7e8a11f7d803"),
      new HttpGet("/examples/jsp/jsp2/tagfiles/hello.jsp","5117780dcff2717856acf34424a37bea277e6d48"),
      new HttpGet("/examples/jsp/jsp2/tagfiles/panel.jsp","dc67adddb6a2b1f97f0d0991034a88bec8a02009"),
      new HttpGet("/examples/jsp/jsp2/tagfiles/products.jsp","96d0f521b94868a2450735a453de19753ce4df6e"),

      // Prints the date - don't validate
      new HttpGet("/examples/jsp/jsp2/jspx/basic.jspx",200),
      new HttpGet("/examples/jsp/jsp2/jspx/svgexample.html",200,"2cc0bde1f9dcdbfdbd2a26ccd7addfce5c332a45"),
      new HttpGet("/examples/jsp/jsp2/jspx/textRotate.jspx?name=JSPX",200,"62d28df98503af07933c280c991d2742edc5f9f9"),
      new HttpGet("/examples/jsp/jsp2/jspattribute/jspattribute.jsp","deafbfc6b6d74013c8684af4ff6caf0f713fcb82"),

      // Shuffle is by definition dynamic
      new HttpGet("/examples/jsp/jsp2/jspattribute/shuffle.jsp",200),
      new HttpGet("/examples/jsp/jsp2/misc/dynamicattrs.jsp","30d42a712e2d336fb6ea84e550d592a38879dde2"),
      new HttpGet("/examples/jsp/jsp2/misc/config.jsp","549154d36f75580e2ab018d6b67113a39cd67788"),

      // Number guessing game
      new NumGuess(),

      // Shows the current time, so can't be digested
      new HttpGet("/examples/jsp/dates/date.jsp"),

      // Can return system-specific data
      new HttpGet("/examples/jsp/snp/snoop.jsp",200),

      new HttpGet("/examples/jsp/error/error.html",200,"7a4eee413a6d4ebc66baca65c1fcf4c2dd1e9904"),
      new HttpGet("/examples/jsp/error/err.jsp?name=audi&submit=Submit",500,"7f207b1e98367e0ee43056f5a242421a46d82eb9"),
      new HttpGet("/examples/jsp/error/err.jsp?name=integra&submit=Submit",200,"13fd4b057c42253a761c0a6499b89d314c09b2c2"),

      /*
       * The following group of methods form a  adding items to a shopping cart
       * and then removing them.  Because each client should have a single persistent
       * session across several iterations through this list, we should leave the session
       * as we find it.
       */
      new HttpGet("/examples/jsp/sessions/carts.html",200,"3754d762eca41dc2eb001f98a4707ea38f2c5433"),
      new HttpPost("/examples/jsp/sessions/carts.jsp?item=NIN+CD&submit=add",200,"3f78ce05a2e8f07aa4f94f19538c28971695d86d"),
      new HttpPost("/examples/jsp/sessions/carts.jsp?item=JSP+Book&submit=add",200,"7ccdb3cec3da190897151937bf3b9c74967d48db"),
      new HttpPost("/examples/jsp/sessions/carts.jsp?item=Love+life&submit=add",200,"5f3ab0d1a3beea9335efe9145f833f9d2c394bdd"),
      new HttpPost("/examples/jsp/sessions/carts.jsp?item=NIN+CD&submit=remove",200,"37eebdc94502e7e68c4dba3ad92ebcea1854a8a5"),
      new HttpPost("/examples/jsp/sessions/carts.jsp?item=JSP+Book&submit=remove",200,"e0ff09f114b563e50348dae09ae51af519318741"),
      new HttpPost("/examples/jsp/sessions/carts.jsp?item=Love+life&submit=remove",200,"3b21b228c817c58439223e190f780704f20798f1"),

      /*
       * The validation here is exhibiting weirdness - TODO fix before release
       *
       * Looks so far like a bug in tomcat :-/
       */
      new HttpGet("/examples/jsp/checkbox/check.html",200,"7a8cff196197a6ed7562a45c7844ac6bf99b02a7"),
      new HttpGet("/examples/jsp/colors/colors.html","9d87b22161bf2849821d81e5e36eb4be18025719"),
      new HttpGet("/examples/jsp/cal/login.html","326bcc06b3ffd9343b9651fcba5f83aaa0866c4d"),

      // Shows the current time, so can't be digested
      new HttpGet("/examples/jsp/include/include.jsp",200),

      // Forwards to one of two files based on VM usage, so can't be digested
      new HttpGet("/examples/jsp/forward/forward.jsp",200),

      // Just shows the "plugin not accepted' response
      new HttpGet("/examples/jsp/plugin/plugin.jsp",200,"6f4fb4e4b400200b1a457d6450009ad1ba2c7c4d"),
      new HttpGet("/examples/jsp/jsptoserv/jsptoservlet.jsp",200,"721d04e5831b5e8a4889d08026b6ed101603b74f"),

      // Writes the date
      new HttpGet("/examples/jsp/xml/xml.jsp",200),
      new HttpGet("/examples/jsp/tagplugin/if.jsp",200,"ca82f131bbb4a0ce4eb4f56260b0e11644a1a467"),
      new HttpGet("/examples/jsp/tagplugin/foreach.jsp",200,"3f4c255def31a491097fcc3047c86fc65cea03e8"),
      new HttpGet("/examples/jsp/tagplugin/choose.jsp",200,"6199be21e61e279106a59479f93221d414f2b689")
  );

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
    this.log = new PrintWriter(new FileWriter(new File(logDir, "client." + ordinal + ".log")));
    this.port = port;
  }

  /**
   * @see java.lang.Runnable#run()
   */
  public void run() {
    final Session session = Session.create(port);
    try {
      for (int i = 0; i < pageCount; i++) {
        for (int p = 0; p < pages.size(); p++) {
          Page page = pages.get(p);
          File logFile = new File(logDir, String.format("result.%d.%d.%d.html", ordinal, p, i));
          boolean result = page.fetch(session, logFile, verbose);
          log.printf("%-50s, %s%n", page.getAddress(), result ? "success" : "fail");
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      session.shutdown();
      log.flush();
    }
  }
}
