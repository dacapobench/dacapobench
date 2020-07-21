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
 *     Apache Software Foundation - org.apache.lucene.demo.IndexFiles
 *     Australian National University - adaptation to DaCapo test harness
 */
package org.dacapo.luindex;

import java.io.BufferedReader;

/**

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

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Paths;
import java.io.FileReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;

import javax.sound.sampled.SourceDataLine;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.index.LogByteSizeMergePolicy;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;

/**
 * date:  $Date: 2009-12-24 11:19:36 +1100 (Thu, 24 Dec 2009) $
 * id: $Id: Index.java 738 2009-12-24 00:19:36Z steveb-oss $
 */
public class Index {

  private final File scratch;
  private final File data;

  public Index(File scratch, File data) {
    this.scratch = scratch;
    this.data = data;
  }

  /**
   * Index all text files under a directory.
   */
  public void indexDir(final File INDEX_DIR, final String[] args) throws IOException {
    IndexWriterConfig IWConfig = new IndexWriterConfig();
    IWConfig.setOpenMode (IndexWriterConfig.OpenMode.CREATE);
    IWConfig.setMergePolicy (new LogByteSizeMergePolicy());
    IndexWriter writer = new IndexWriter(FSDirectory.open(Paths.get(INDEX_DIR.getCanonicalPath())), IWConfig);
    for (int arg = 0; arg < args.length; arg++) {
      final File docDir = new File(args[arg]);
      if (!docDir.exists() || !docDir.canRead()) {
        System.out.println("Document directory '" + docDir.getAbsolutePath() + "' does not exist or is not readable, please check the path");
        throw new IOException("Cannot read from document directory");
      }
      File prefix = docDir.getAbsolutePath().contains(scratch.getAbsolutePath()) ? scratch : data;
      System.out.print("Indexing "+docDir.getName()+"... ");
      indexDocs(writer, docDir, prefix);

      System.out.println("Optimizing...");
      writer.forceMerge(1);
    }
    writer.close();
  }
  /**
   * Takes in a merged one-document-per-line text file from Lucene Wikipedia output,
   * and index documents there.
   */
  public void indexLineDoc(final File INDEX_DIR, final String[] args) throws IOException {
    IndexWriterConfig IWConfig = new IndexWriterConfig();
    IWConfig.setOpenMode (IndexWriterConfig.OpenMode.CREATE);
    IWConfig.setMergePolicy (new LogByteSizeMergePolicy());
    IndexWriter writer = new IndexWriter(FSDirectory.open(Paths.get(INDEX_DIR.getCanonicalPath())), IWConfig);

    for (int idx = 0; idx < args.length; idx ++) {
      File txtFile = new File(args[idx]);

      if (!txtFile.exists() || !txtFile.canRead()) {
        System.out.println("Document directory '" + txtFile.getAbsolutePath() + "' does not exist or is not readable, please check the path");
        throw new IOException("Cannot read from document directory");
      }

      BufferedReader reader = new BufferedReader(new FileReader(txtFile));
      String line = reader.readLine();
      int n = 0;
      String lead = "Adding documents from "+txtFile.getName()+": ";

      System.out.print(lead+"\r");
      while (line != null) {
        writer.addDocument(getLuceneDocFromLine(line));
        line = reader.readLine();
        n ++;
        if (n % 1000 == 0) System.out.print(lead+n+"\r");
      }
      System.out.println(lead+n);
    }

    System.out.println("Optimizing...");
    writer.forceMerge(1);
    writer.close();
  }

  private final char SEP = '\t';

  Document getLuceneDocFromLine(String line) {
    Document doc = new Document();
    FieldType defaultFT = new FieldType();
    defaultFT.setTokenized (false);
    defaultFT.setStored (true);
    defaultFT.setIndexOptions (IndexOptions.DOCS);

    int spot = line.indexOf(SEP);
    int spot2 = line.indexOf(SEP, 1 + spot);
    int spot3 = line.indexOf(SEP, 1 + spot2);
    if (spot3 == -1) {
      spot3 = line.length();
    }

    // Add title as a field. Use a field that is
    // indexed (i.e. searchable), but don't tokenize the field into words.
    doc.add(new Field("title", line.substring(0, spot), defaultFT));

    // Add date as a field. Indexed, but not tokenized.
    doc.add(new Field("modified", line.substring(1+spot, spot2), defaultFT));

    // Add body as a field. Tokenized and indexed, but not stored.
    FieldType bodyFT = new FieldType();
    bodyFT.setTokenized(true);
    bodyFT.setStored(false);
    bodyFT.setIndexOptions(IndexOptions.DOCS);
    doc.add(new Field("contents", line.substring(1 + spot2, spot3), bodyFT));

    return doc;
  }

  /**
   * Index either a file or a directory tree.
   * 
   * @param writer
   * @param file
   * @throws IOException
   */
  void indexDocs(IndexWriter writer, File file, File prefix) throws IOException {

    /* Strip the absolute part of the path name from file name output */
    int prefixIdx = prefix.getCanonicalPath().length() + 1;

    /* do not try to index files that cannot be read */
    if (file.canRead()) {
      if (file.isDirectory()) {
        String[] files = file.list();
        // an IO error could occur
        if (files != null) {
          System.out.print(file.getName()+" ("+files.length+") ");
          Arrays.sort(files);
          for (int i = 0; i < files.length; i++) {
            indexDocs(writer, new File(file, files[i]), prefix);
          }
        }
      } else {
        // System.out.println("adding " + file.getCanonicalPath().substring(prefixIdx));
        try {
          Document doc = new Document();
          FieldType docFT = new FieldType();
          docFT.setTokenized (false);
          docFT.setStored (true);
          docFT.setIndexOptions (IndexOptions.DOCS);

          // Add the path of the file as a field named "path".  Use a field that is
          // indexed (i.e. searchable), but don't tokenize the field into words.
          doc.add(new Field("path", file.getPath(), docFT));

          // Add the last modified date of the file a field named "modified".  Use
          // a field that is indexed (i.e. searchable), but don't tokenize the field
          // into words.
          doc.add(new Field("modified",
                  DateTools.timeToString(file.lastModified(), DateTools.Resolution.MINUTE),
                  docFT));

          // Add the contents of the file to a field named "contents".  Specify a Reader,
          // so that the text of the file is tokenized and indexed, but not stored.
          // Note that FileReader expects the file to be in the system's default encoding.
          // If that's not the case searching for special characters will fail.
          docFT.setTokenized (true);
          docFT.setStored (false);
          doc.add(new Field("contents", new FileReader(file), docFT));
          writer.addDocument(doc);
        }
        // at least on windows, some temporary files raise this exception with
        // an "access denied" message
        // checking if the file can be read doesn't help
        catch (FileNotFoundException fnfe) { }
      }
    }
  }
}
