/*
 * Copyright (c) 2006, 2009 The Australian National University.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License v2.0.
 * You may obtain the license at
 * 
 *    http://www.opensource.org/licenses/apache2.0.php
 */
package org.dacapo.harness;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

import org.dacapo.parser.Config;

/**
 * Dump the key info fields for each benchmark into a latex file
 * 
 * @date $Date: 2009-12-24 11:19:36 +1100 (Thu, 24 Dec 2009) $
 * @id $Id: LatexDescriptions.java 738 2009-12-24 00:19:36Z steveb-oss $
 */
public class LatexDescriptions {
  private static String[] items = { "short", "long", "threads", "repeats", "author", "license", "copyright", "url", "version" };

  public static void main(String[] args) {
    try {
      print(args, System.out);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static void print(String[] bms, String filename) throws IOException {
    print(bms, new PrintStream(new BufferedOutputStream(new FileOutputStream(filename))));
  }

  public static void print(String[] bms, PrintStream out) throws IOException {
    Config[] configs = new Config[bms.length];
    for (int j = 0; j < bms.length; j++) {
      InputStream ins = LatexDescriptions.class.getClassLoader().getResourceAsStream("cnf/" + bms[j] + ".cnf");
      configs[j] = Config.parse(ins);
      ins.close();
    }
    for (int i = 0; i < items.length; i++) {
      out.println("\\newcommand{\\bm" + strop(items[i]) + "}[1]{%");
      for (int j = 0; j < bms.length; j++) {
        out.print("\\ifthenelse{\\equal{#1}{" + bms[j] + "}}");
        out.println("{" + configs[j].getDesc(items[i]) + "}{}%");
      }
      out.println("}");
    }
    out.close();
  }

  private static String strop(String s) {
    char[] c = s.toCharArray();
    c[0] = Character.toUpperCase(c[0]);
    return new String(c);
  }
}
