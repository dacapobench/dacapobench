/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Apache Software Foundation
 *     Australian National University - adaptation to DaCapo test harness
 */
package org.dacapo.lusearch;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Paths;

import org.dacapo.harness.LatencyReporter;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.search.TopDocsCollector;
import org.apache.lucene.search.TopScoreDocCollector;

/**
 * Simple command-line based search demo.
 *
 * date:  $Date: 2009-12-24 11:19:36 +1100 (Thu, 24 Dec 2009) $
 * id: $Id: Search.java 738 2009-12-24 00:19:36Z steveb-oss $
 */
public class Search {

  static final int MAX_DOCS_TO_COLLECT = 20;
  public int completed = 0;

  public Search() {
  }

  /** Simple command-line based search demo. */
  public void main(String[] args) throws Exception {
    String usage = "Usage:\tjava org.dacapo.lusearch.Search [-ind" +
            "ex dir] [-field f] [-iterations n] [-queries file] [-raw] [-norms field] [-paging hitsPerPage]";
    usage += "\n\tSpecify 'false' for hitsPerPage to use streaming instead of paging search.";
    if (args.length > 0 && ("-h".equals(args[0]) || "-help".equals(args[0]))) {
      System.out.println(usage);
      System.exit(0);
    }

    String index = "index";
    String field = "contents";
    String queryBase = null;
    int iterations = 1;
    boolean raw = false;
    String normsField = null;
    int hitsPerPage = 10;
    String outBase = null;
    int threads = 1;
    int totalQuerieSets = 32;
    int querySetSize = 256;

    for (int i = 0; i < args.length; i++) {
      if ("-index".equals(args[i])) {
        index = args[i + 1];
        i++;
      } else if ("-field".equals(args[i])) {
        field = args[i + 1];
        i++;
      } else if ("-queries".equals(args[i])) {
        queryBase = args[i + 1];
        i++;
      } else if ("-iterations".equals(args[i])) {
        iterations = Integer.parseInt(args[i + 1]);
        i++;
      } else if ("-raw".equals(args[i])) {
        raw = true;
      } else if ("-norms".equals(args[i])) {
        normsField = args[i + 1];
        i++;
      } else if ("-paging".equals(args[i])) {
        hitsPerPage = Integer.parseInt(args[i + 1]);
        i++;
      } else if ("-output".equals(args[i])) {
        outBase = args[i + 1];
        i++;
      } else if ("-threads".equals(args[i])) {
        threads = Integer.parseInt(args[i + 1]);
        i++;
      } else if ("-totalquerysets".equals(args[i])) {
        totalQuerieSets = Integer.parseInt(args[i + 1]);
        i++;
      } else if ("-querysetsize".equals(args[i])) {
        querySetSize = Integer.parseInt(args[i + 1]);
        i++;
      }
    }
    completed = 0;
    int totalQueries = totalQuerieSets * iterations * querySetSize;
    LatencyReporter.initialize(totalQueries, threads, querySetSize);
    LatencyReporter.requestsStarting();

    try {
      DirectoryReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(index)));
      IndexSearcher searcher = new IndexSearcher(reader);

      for (int j = 0; j < threads; j++) {
        new QueryThread(this, reader, searcher, "Query" + j, j, threads, totalQuerieSets, outBase, queryBase, field, raw, hitsPerPage, iterations).start();
      }

      synchronized (this) {
        while (completed != totalQuerieSets*iterations) {
          try {
            this.wait();
          } catch (InterruptedException e) {
          }
        }
        System.out.println();
      }
      reader.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
    LatencyReporter.requestsFinished();
  }

  class QueryThread extends Thread {

    final Search parent;
    final DirectoryReader reader;
    final IndexSearcher searcher;
    final int threadID;
    final int threadCount;
    final int totalQueries;
    final String name;
    final String outBase;
    final String queryBase;
    final String field;
    final boolean raw;
    final int hitsPerPage;
    final int iterations;

    public QueryThread(Search parent, DirectoryReader reader, IndexSearcher searcher, String name, int threadID, int threadCount, int totalQueries, String outBase, String queryBase, String field, boolean raw, int hitsPerPage, int iterations) {
      super(name);
      this.parent = parent;
      this.reader = reader;
      this.searcher = searcher;
      this.threadID = threadID;
      this.threadCount = threadCount;
      this.totalQueries = totalQueries;
      this.name = name;
      this.outBase = outBase;
      this.queryBase = queryBase;
      this.field = field;
      this.raw = raw;
      this.hitsPerPage = hitsPerPage;
      this.iterations = iterations;
    }

    public void run() {
      try {
        int count = totalQueries / threadCount + (threadID < (totalQueries % threadCount) ? 1 : 0);
        for (int r = 0; r < iterations; r++) {
          for (int i = 0, queryId = threadID; i < count; i++, queryId += threadCount) {
            // make and run query
            new QueryProcessor(parent, reader, searcher, queryId, outBase, queryBase, field, raw, hitsPerPage, totalQueries, iterations, threadID).run();
          }
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

  }

  public class QueryProcessor {
    final Search parent;
    final String field;
    final DirectoryReader reader;
    final IndexSearcher searcher;
    final int hitsPerPage;
    final boolean raw;
    BufferedReader in;
    PrintWriter out;
    final int iterations;
    final int fivePercent;
    final int threadID;

    public QueryProcessor(Search parent, DirectoryReader reader, IndexSearcher searcher, int queryID, String outBase, String queryBase, String field, boolean raw,
        int hitsPerPage, int totalQueries, int iterations, int threadID) {
      this.parent = parent;
      this.reader = reader;
      this.searcher = searcher;
      this.threadID = threadID;
      this.field = field;
      this.raw = raw;
      this.hitsPerPage = hitsPerPage;
      this.fivePercent = iterations*totalQueries/20;
      this.iterations = iterations;
      try {
        String query = queryBase + File.separator + "query" + (queryID < 10 ? "000" : (queryID < 100 ? "00" : (queryID < 1000 ? "0" : ""))) + queryID + ".txt";
        in = new BufferedReader(new FileReader(query));
        out = new PrintWriter(new BufferedWriter(new FileWriter(outBase + queryID)));
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    public void run() throws java.io.IOException {
      Analyzer analyzer = new StandardAnalyzer();
      QueryParser parser = new QueryParser(field, analyzer);

      while (true) {
        String line = in.readLine();

        if (line == null || line.length() == -1)
          break;

        line = line.trim();
        if (line.length() == 0)
          break;

        LatencyReporter.start(threadID);
        if (line.equals("OR") || line.equals("AND") || line.equals("NOT") || line.equals("TO"))
          line = line.toLowerCase();

        Query query = null;
        try {
          query = parser.parse(line);
        } catch (Exception e) {
          System.err.println("Failed to process query: '"+line+"'");
          e.printStackTrace();
        }
        searcher.search(query, 10);

        doPagingSearch(query);
        LatencyReporter.end(threadID);
      }

      in.close();
      out.flush();
      out.close();
      synchronized (parent) {
        parent.completed++;
        if (fivePercent == 0)
          System.out.print("Completing query batches: "+parent.completed+"\r");
        else if (parent.completed % fivePercent == 0) {
          int percentage = 5 * (parent.completed / fivePercent);
          System.out.print("Completing query batches: "+percentage+"%\r");
        }
        parent.notify();
      }
    }

    /**
     * This demonstrates a typical paging search scenario, where the search
     * engine presents pages of size n to the user. The user can then go to the
     * next page if interested in the next hits.
     *
     * When the query is executed for the first time, then only enough results
     * are collected to fill 5 result pages. If the user wants to page beyond
     * this limit, then the query is executed another time and all hits are
     * collected.
     *
     */
    public void doPagingSearch(Query query) throws IOException {

      // Collect enough docs to show 5 pages
      TopDocsCollector<ScoreDoc> collector =  TopScoreDocCollector.create(MAX_DOCS_TO_COLLECT, Integer.MAX_VALUE);
      searcher.search(query, collector);
      ScoreDoc[] hits = collector.topDocs().scoreDocs;

      int numTotalHits = collector.getTotalHits();
      if (numTotalHits > 0)
        out.println(numTotalHits + " total matching documents for " + query.toString(field));

      int start = 0;
      int end = Math.min(numTotalHits, hitsPerPage);

      while (start < Math.min(MAX_DOCS_TO_COLLECT, numTotalHits)) {
        end = Math.min(hits.length, start + hitsPerPage);

        for (int i = start; i < end; i++) {
          if (raw) { // output raw format
            out.println("doc=" + hits[i].doc + " score=" + hits[i].score);
            continue;
          }

          Document doc = searcher.doc(hits[i].doc);
          String path = doc.get("path");
          if (path != null) {
            out.println("\t" + (i + 1) + ". " + path);
            String title = doc.get("title");
            if (title != null) {
              out.println("   Title: " + doc.get("title"));
            }
          } else {
            out.println((i + 1) + ". " + "No path for this document");
          }

        }
        start = end;
      }
    }
  }
}
