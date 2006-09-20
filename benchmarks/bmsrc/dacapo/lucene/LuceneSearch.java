package dacapo.lucene;

/**
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.FilterIndexReader;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Hits;
import org.apache.lucene.queryParser.QueryParser;

import dacapo.parser.Config;

/** Simple command-line based search demo. */
public class LuceneSearch extends dacapo.Benchmark {
  public int completed = 0;
  
  /** Use the norms from one field for all fields.  Norms are read into memory,
   * using a byte of memory per document per searched field.  This can cause
   * search of large collections with a large number of fields to run out of
   * memory.  If all of the fields contain only a single token, then the norms
   * are all identical, then single norm vector may be shared. */
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
  
  
  
  public LuceneSearch(Config config, File scratch) throws Exception {
    super(config, scratch);
  }
  
  
  
  /** Simple command-line based search demo. */
  public void iterate(String size) throws Exception {
    String[] args = config.getArgs(size);
    String usage =
      "Usage: java org.apache.lucene.demo.SearchFiles [-index dir] [-field f] [-repeat n] [-queries file] [-raw] [-norms field]";
    if (args.length > 0 && ("-h".equals(args[0]) || "-help".equals(args[0]))) {
      System.out.println(usage);
      System.exit(0);
    }
    
    String index = "index";
    String field = "contents";
    String queryBase = null;
    String outBase = null;
    int repeat = 0;
    boolean raw = false;
    String normsField = null;
    int threads = 1;
    
    for (int i = 0; i < args.length; i++) {
      if ("-index".equals(args[i])) {
        index = fileInScratch(args[i+1]);
        i++;
      } else if ("-field".equals(args[i])) {
        field = args[i+1];
        i++;
      } else if ("-queries".equals(args[i])) {
        queryBase = fileInScratch(args[i+1]);
        i++;
      } else if ("-repeat".equals(args[i])) {
        repeat = Integer.parseInt(args[i+1]);
        i++;
      } else if ("-raw".equals(args[i])) {
        raw = true;
      } else if ("-norms".equals(args[i])) {
        normsField = args[i+1];
        i++;
      } else if ("-threads".equals(args[i])) {
        threads = Integer.parseInt(args[++i]);
      } else if ("-output".equals(args[i])) {
        outBase = args[++i];
      }
    }
    completed = 0;
    for (int j = 0; j < threads; j++) {
      new QueryThread("Query"+j, j, index, outBase, queryBase, field, normsField, repeat, raw, this).start();
    }
    synchronized (this) {
      while (completed != threads) {
        try {
          this.wait();
        } catch (InterruptedException e) { }
      }
    }
  }
  
  
  public class QueryThread extends Thread {
    Searcher searcher;
    BufferedReader in;
    PrintWriter out;
    IndexReader reader;
    String field;
    int repeat;
    boolean raw;
    LuceneSearch parent;
    
    public QueryThread(String str, int id, String index, String outBase, String queryBase,
        String field, String normsField, int repeat, boolean raw, LuceneSearch parent) {
      super(str);
      try {
        reader = IndexReader.open(index);
        this.field = field;
        if (normsField != null) reader = new OneNormsReader(reader, normsField);
        searcher = new IndexSearcher(reader);
        in = new BufferedReader(new FileReader(queryBase + id + ".txt"));
        out = new PrintWriter(new BufferedWriter(new FileWriter(fileInScratch(outBase + id))));
      } catch (Exception e) {
        e.printStackTrace();
      }
      this.repeat = repeat;
      this.raw = raw;
      this.parent = parent;
    }
    
    public void run() {
      try {
        runQuery();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    
    private void runQuery() throws java.io.IOException {
      Analyzer analyzer = new StandardAnalyzer();
      
      while (true) {
        String line = in.readLine();
        
        if (line == null || line.length() == -1)
          break;
        
        Query query = null;
        try {
          query = QueryParser.parse(line, field, analyzer);
        } catch (Exception e){
          e.printStackTrace(); 
        }
        out.println("Searching for: " + query.toString(field));
        
        Hits hits = searcher.search(query);
        
        if (repeat > 0) {                           // repeat & time as benchmark
          for (int i = 0; i < repeat; i++) {
            hits = searcher.search(query);
          }
        }
        
        out.println(hits.length() + " total matching documents");
        
        final int HITS_PER_PAGE = 10;
        for (int start = 0; start < hits.length(); start += HITS_PER_PAGE) {
          int end = Math.min(hits.length(), start + HITS_PER_PAGE);
          for (int i = start; i < end; i++) {
            
            if (raw) {                              // output raw format
              out.println("doc="+hits.id(i)+" score="+hits.score(i));
              continue;
            }
            
            Document doc = hits.doc(i);
            String path = doc.get("path");
            if (path != null) {
              out.println((i+1) + ". " + path);
              String title = doc.get("title");
              if (title != null) {
                out.println("   Title: " + doc.get("title"));
              }
            } else {
              out.println((i+1) + ". " + "No path for this document");
            }
          }
          
          break;
        }
      }
      out.flush();
      out.close();
      reader.close();
      synchronized (parent) {
        parent.completed++;
        if (parent.completed % 4 == 0) {
          System.out.println(parent.completed+" query batches completed");
        }
        parent.notify();
      }
    }
  }
}

