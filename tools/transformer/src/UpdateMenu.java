/*
 * Copyright (c) 2009 The Australian National University.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License v2.0.
 * You may obtain the license at
 * 
 *    http://www.opensource.org/licenses/apache2.0.php
 *
 * Simple transformer tool to apply an xslt to an xml file to yield a 
 * transformed result.
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;


public final class UpdateMenu {

	private static final String MENU_COMMENT_LINE = "<!-- ADD BENCHMARK HERE -->";
	private static final String MENU_URL_LINE     = "<LI><A HREF=\"%s/%s.html\" TARGET=\"MAIN\">%s</A></LI>";
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws IOException {
		if (args.length < 2 || 3 < args.length) {
			reportUsage();
			System.exit(1);
		}

		try {
			File            inputFile = new File(args[0]);
			FileInputStream is        = new FileInputStream(inputFile);
			BufferedReader  reader    = new BufferedReader(new InputStreamReader(is));
			String          benchmark = args[1];
			PrintStream     os        = System.out;

			if (args.length == 3) {
				os = new PrintStream(new File(args[2]));
			}
			
			String line = null;
			
			while ((line = reader.readLine()) != null) {
				if (line.equals(MENU_COMMENT_LINE)) {
					os.printf(MENU_URL_LINE, benchmark,benchmark,benchmark);
					os.println();
				}
				os.println(line);
			}
			
			os.close();
			reader.close();
		} catch (FileNotFoundException e) {
			reportUsage();
		}
	}

	private static void reportUsage() {
		System.err.println("usage:");
		System.err.println("    java " + UpdateMenu.class.toString() + " <input> <bm> [<output>]");
		System.err.println("  <in>       the input file that the bm will be added to.");
		System.err.println("  <bm>       the name of the bm to add.");
		System.err.println("  [<output>] the file the transformed xml is written to (if unspecified then");
		System.err.println("             the output is written to stdout).");
	}
}
