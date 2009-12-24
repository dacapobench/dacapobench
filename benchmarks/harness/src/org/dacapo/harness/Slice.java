/*
 * Copyright (c) 2006, 2009 The Australian National University.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License v2.0.
 * You may obtain the license at
 * 
 *    http://www.opensource.org/licenses/apache2.0.php
 */
package org.dacapo.harness;

import java.io.*;

/**
 * Slice a set of files into a larger set (used to produce the input set that is
 * used in the lusearch benchmark).
 * 
 * Slice(1) will produce the original file. Slice(2) will produce Slice(1) and
 * two halves of the original file. Slice(3) will produce a) 3 files 1/3 of the
 * size of the input file b) 2 files, 2/3 of the size of the input file c) 1
 * file 3/3 of the size. and so on.
 * 
 * @date $Date: 2009-12-24 11:19:36 +1100 (Thu, 24 Dec 2009) $
 * @id $Id: Slice.java 738 2009-12-24 00:19:36Z steveb-oss $
 */
public class Slice {

  /**
   * Extract a section of a file, starting at the given line number and of the
   * given length.
   * 
   * @param inFile Input file name
   * @param outFile Output file name
   * @param offset The line number at which to start extracting
   * @param length The length in lines of the extract
   * @throws IOException
   */
  private static void extract(File inFile, File outFile, int offset, int length) throws IOException {
    BufferedReader in = new BufferedReader(new FileReader(inFile));
    PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(outFile)));
    int lineNo = 0;
    String line;
    while ((line = in.readLine()) != null && lineNo < offset + length) {
      if (offset <= lineNo && lineNo < offset + length)
        out.println(line);
      lineNo++;
    }
    in.close();
    out.close();
  }

  /**
   * Slice a file into a number of sub files. Output in the given directory.
   * Files are called <input-file>.<fraction>.<numerator>.<index> such that file
   * "x.4.3.1" is the first file, 3/4 of the size of the input file.
   * 
   * @param file File to slice
   * @param dir Destination directory
   * @param fraction Denominator to drive the slicing operation
   * @throws IOException
   */
  public static void slice(File file, File dir, int fraction) throws IOException {
    int length = Benchmark.lineCount(file);
    int lines = (length) / fraction; // round up

    /* iterate over the length of the output file */
    for (int pass = 1; pass <= fraction; pass++) {
      int chunk = pass * lines; // Length of the sub-file at this pass

      /* Iterate over the offset into the initial file */
      for (int offset = 0, n = 0; offset <= length - chunk; offset += lines, n++) {
        File outFile = new File(dir, file.getName() + "." + fraction + "." + pass + "." + n);
        extract(file, outFile, offset, chunk);
      }
    }
  }

  /**
   * @param args file-to-slice destination-dir fraction
   */
  public static void main(String[] args) throws IOException {
    String file = args[0];
    String targetDir = args[1];
    int fraction = Integer.parseInt(args[2]);

    slice(new File(file), new File(targetDir), fraction);
  }
}