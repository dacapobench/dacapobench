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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.demo.FileDocument;

import org.dacapo.harness.DacapoException;
import org.dacapo.parser.Config;

/**
 * @date $Date: 2009-12-24 11:19:36 +1100 (Thu, 24 Dec 2009) $
 * @id $Id: Index.java 738 2009-12-24 00:19:36Z steveb-oss $
 */
public class Index {

  private final File scratch;

  public Index(File scratch) {
    this.scratch = scratch;
  }

  /**
   * Index all text files under a directory.
   */
  public void main(final File INDEX_DIR, final String[] args) throws DacapoException, IOException {
    IndexWriter writer = new IndexWriter(INDEX_DIR, new StandardAnalyzer(), true, IndexWriter.MaxFieldLength.LIMITED);
    for (int arg = 0; arg < args.length; arg++) {
      final File docDir = new File(scratch, args[arg]);
      if (!docDir.exists() || !docDir.canRead()) {
        System.out.println("Document directory '" + docDir.getAbsolutePath() + "' does not exist or is not readable, please check the path");
        throw new DacapoException("Cannot read from document directory");
      }

      indexDocs(writer, docDir);
      System.out.println("Optimizing...");
      writer.optimize();
    }
    writer.close();
  }

  /**
   * Index either a file or a directory tree.
   * 
   * @param writer
   * @param file
   * @throws IOException
   */
  void indexDocs(IndexWriter writer, File file) throws IOException {

    /* Strip the absolute part of the path name from file name output */
    int scratchP = scratch.getPath().length() + 1;

    /* do not try to index files that cannot be read */
    if (file.canRead()) {
      if (file.isDirectory()) {
        String[] files = file.list();
        // an IO error could occur
        if (files != null) {
          Arrays.sort(files);
          for (int i = 0; i < files.length; i++) {
            indexDocs(writer, new File(file, files[i]));
          }
        }
      } else {
        System.out.println("adding " + file.getPath().substring(scratchP));
        try {
          writer.addDocument(FileDocument.Document(file));
        }
        // at least on windows, some temporary files raise this exception with
        // an "access denied" message
        // checking if the file can be read doesn't help
        catch (FileNotFoundException fnfe) {
          ;
        }
      }
    }
  }
}
