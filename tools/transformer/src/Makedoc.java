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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;

import javax.xml.transform.*;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;

import org.xml.sax.InputSource;


public class Makedoc {
	/**
	 * @param args
	 * @throws FileNotFoundException 
	 * @throws TransformerException 
	 */
	public static void main(String[] args) throws TransformerException {
		if (args.length < 2 || 3 < args.length) {
			reportUsage();
			System.exit(1);
		}
		
		try {
			File         xsltFile = new File(args[0]);
			File         xmlFile  = new File(args[1]);
			OutputStream os       = System.out;
		
			if (args.length == 3) {
				File outFile = new File(args[2]);
				
				os = new FileOutputStream(outFile);
			}
			
			TransformerFactory factory = TransformerFactory.newInstance();
			
			SAXSource xsltSource = new SAXSource(new InputSource(new FileInputStream(xsltFile)));
			SAXSource xmlSource  = new SAXSource(new InputSource(new FileInputStream(xmlFile)));
			
			Transformer transformer = factory.newTransformer(xsltSource);
			
			StreamResult outputTarget = new StreamResult(os);
			
			transformer.transform(xmlSource, outputTarget);
		} catch (FileNotFoundException fnfe) {
			reportUsage();
			System.exit(10);
		}
	}

	private static void reportUsage() {
		System.err.println("usage:");
		System.err.println("    java " + Makedoc.class.toString() + " <xslt> <xml> [<output>]");
		System.err.println("  <xslt>     the input xlst file that used to transform the xml file.");
		System.err.println("  <xml>      the input xml file that is transformed according to the xslt file.");
		System.err.println("  [<output>] the file the transformed xml is written to (if unspecified then");
		System.err.println("             the output is written to stdout).");
	}
}
