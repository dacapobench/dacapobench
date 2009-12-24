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
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.FilterIndexReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.TopDocCollector;

/**
 * Simple command-line based search demo.
 * 
 * @date $Date: 2009-12-24 11:19:36 +1100 (Thu, 24 Dec 2009) $
 * @id $Id: Search.java 738 2009-12-24 00:19:36Z steveb-oss $
 */
public class Search {

  static final int MAX_DOCS_TO_COLLECT = 20;
  public int completed = 0;

  /**
   * Use the norms from one field for all fields. Norms are read into memory,
   * using a byte of memory per document per searched field. This can cause
   * search of large collections with a large number of fields to run out of
   * memory. If all of the fields contain only a single token, then the norms
   * are all identical, then single norm vector may be shared.
   */
  private static class OneNormsReader extends FilterIndexReader {
    private String field;

    public OneNormsReader(IndexReader in, String field) {
      super(in);
      this.field = field;
    }

    public byte[] norms(String field) throws IOException {
      return in.norms(this.field);
    }
  }

  public Search() {
  }

  /** Simple command-line based search demo. */
  public void main(String[] args) throws Exception {
    String usage = "Usage:\tjava org.dacapo.lusearch.Search [-index dir] [-field f] [-repeat n] [-queries file] [-raw] [-norms field] [-paging hitsPerPage]";
    usage += "\n\tSpecify 'false' for hitsPerPage to use streaming instead of paging search.";
    if (args.length > 0 && ("-h".equals(args[0]) || "-help".equals(args[0]))) {
      System.out.println(usage);
      System.exit(0);
    }

    String index = "index";
    String field = "contents";
    String queryBase = null;
    int repeat = 0;
    boolean raw = false;
    String normsField = null;
    int hitsPerPage = 10;
    String outBase = null;
    int threads = 1;
    int totalQueries = 32;

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
      } else if ("-repeat".equals(args[i])) {
        repeat = Integer.parseInt(args[i + 1]);
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
      } else if ("-totalqueries".equals(args[i])) {
        totalQueries = Integer.parseInt(args[i + 1]);
        i++;
      }
    }
    completed = 0;
    for (int j = 0; j < threads; j++) {
      new QueryThread(this, "Query" + j, j, threads, totalQueries, index, outBase, queryBase, field, normsField, raw, hitsPerPage).start();
    }
    synchronized (this) {
      while (completed != totalQueries) {
        try {
          this.wait();
        } catch (InterruptedException e) {
        }
      }
    }
  }

  class QueryThread extends Thread {

    Search parent;
    int id;
    int threadCount;
    int totalQueries;
    String name;
    String index;
    String outBase;
    String queryBase;
    String field;
    String normsField;
    boolean raw;
    int hitsPerPage;

    public QueryThread(Search parent, String name, int id, int threadCount, int totalQueries, String index, String outBase, String queryBase, String field,
        String normsField, boolean raw, int hitsPerPage) {
      super(name);
      this.parent = parent;
      this.id = id;
      this.threadCount = threadCount;
      this.totalQueries = totalQueries;
      this.name = name;
      this.index = index;
      this.outBase = outBase;
      this.queryBase = queryBase;
      this.field = field;
      this.normsField = normsField;
      this.raw = raw;
      this.hitsPerPage = hitsPerPage;
    }

    public void run() {
      try {
        int count = totalQueries / threadCount + (id < (totalQueries % threadCount) ? 1 : 0);
        for (int i = 0, queryId = id; i < count; i++, queryId += threadCount) {
          // make and run query
          new QueryProcessor(parent, name, queryId, index, outBase, queryBase, field, normsField, raw, hitsPerPage).run();
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

  }

  public class QueryProcessor {

    Search parent;
    String field;
    int hitsPerPage;
    boolean raw;

    IndexReader reader;
    Searcher searcher;
    BufferedReader in;
    PrintWriter out;

    public QueryProcessor(Search parent, String name, int id, String index, String outBase, String queryBase, String field, String normsField, boolean raw,
        int hitsPerPage) {
      this.parent = parent;
      this.field = field;
      this.raw = raw;
      this.hitsPerPage = hitsPerPage;
      try {
        reader = IndexReader.open(index);
        if (normsField != null)
          reader = new OneNormsReader(reader, normsField);
        searcher = new IndexSearcher(reader);

        String query = queryBase + (id < 10 ? "00" : (id < 100 ? "0" : "")) + id + ".txt";
        in = new BufferedReader(new FileReader(query));
        out = new PrintWriter(new BufferedWriter(new FileWriter(outBase + id)));

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

        Query query = null;
        try {
          query = parser.parse(line);
        } catch (Exception e) {
          e.printStackTrace();
        }
        searcher.search(query, null, 10);

        doPagingSearch(query);
      }

      reader.close();
      out.flush();
      out.close();
      synchronized (parent) {
        parent.completed++;
        if (parent.completed % 4 == 0) {
          System.out.println(parent.completed + " query batches completed");
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
      TopDocCollector collector = new TopDocCollector(MAX_DOCS_TO_COLLECT);
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
