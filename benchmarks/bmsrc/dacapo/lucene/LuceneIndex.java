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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Date;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.demo.FileDocument;

import dacapo.Benchmark;
import dacapo.DacapoException;
import dacapo.parser.Config;

/**
 * Dacapo benchmark harness for Lucene.  This is a modified version of
 * org.apache.lucene.demo.IndexFiles, as distributed with Lucene.
 * 
 * @author Apache
 * @author Robin Garner
 *
 */

public class LuceneIndex extends Benchmark {
  
  public LuceneIndex(Config config, File scratch) throws Exception {
    super(config,scratch);
  }
  
  public void cleanup() {
    if (!preserve) {
      deleteTree(new File(scratch,"luindex"));
    }
  }
  
  public void postIteration(String size) {
    if (!preserve) {
      deleteTree(new File(scratch,"index"));
    }
  }
  
  /** Index all text files under a directory. */
  public void iterate(String size) throws DacapoException, IOException {
    String[] args = config.getArgs(size);
    
    final File INDEX_DIR = new File(scratch,"index");
    
    if (INDEX_DIR.exists()) {
      System.out.println("Cannot save index to '" +INDEX_DIR+ "' directory, please delete it first");
      throw new DacapoException("Cannot write to index directory");
    }

    
    IndexWriter writer = new IndexWriter(INDEX_DIR, new StandardAnalyzer(), true);
    for ( int arg = 0; arg < args.length; arg++) {
      final File docDir = new File(scratch,args[arg]);
      if (!docDir.exists() || !docDir.canRead()) {
        System.out.println("Document directory '" +docDir.getAbsolutePath()+ "' does not exist or is not readable, please check the path");
        throw new DacapoException("Cannot read from document directory");
      }

      indexDocs(writer, docDir);
      System.out.println("Optimizing...");
      writer.optimize();
    }
    writer.close();
  }

  void indexDocs(IndexWriter writer, File file)
    throws IOException {

    /* Strip the absolute part of the path name from file name output */
    int scratchP = scratch.getPath().length()+1;
    
    /* do not try to index files that cannot be read */
    if (file.canRead()) {
      if (file.isDirectory()) {
        String[] files = file.list();
        // an IO error could occur
        if (files != null) {
          for (int i = 0; i < files.length; i++) {
            indexDocs(writer, new File(file, files[i]));
          }
        }
      } else {
        System.out.println("adding " + file.getPath().substring(scratchP));
        try {
          writer.addDocument(FileDocument.Document(file));
        }
        // at least on windows, some temporary files raise this exception with an "access denied" message
        // checking if the file can be read doesn't help
        catch (FileNotFoundException fnfe) {
          ;
        }
      }
    }
  }
}
