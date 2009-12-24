/*
 * Copyright (c) 2005, 2009 The Australian National University.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License v2.0.
 * You may obtain the license at
 * 
 *    http://www.opensource.org/licenses/apache2.0.php
 */
package org.dacapo.xalan;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Properties;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

/**
 * @date $Date: 2009-12-24 11:19:36 +1100 (Thu, 24 Dec 2009) $
 * @id $Id: XSLTBench.java 738 2009-12-24 00:19:36Z steveb-oss $
 */
public class XSLTBench {

  final boolean verbose = false;

  // What version of XALAN should we have
  private final String XALAN_VERSION = "Xalan Java 2.7.1";
  private final File scratch;

  int workers;

  /*
   * A simple queue of filenames that the worker threads pull jobs from.
   */
  class WorkQueue {
    LinkedList<String> _queue = new LinkedList<String>();

    public synchronized void push(String filename) {
      if (verbose)
        System.out.println("workQueue.push");
      _queue.add(filename);
      notify();
    }

    public synchronized String pop() {
      while (_queue.isEmpty()) {
        try {
          wait();
        } catch (InterruptedException e) {
        }
        if (verbose)
          System.out.println("workQueue.pop");
      }
      return _queue.removeFirst();
    }
  }

  /*
   * Worker thread. Provided with a queue that input files can be selected from
   * and a template object that can be used to perform a transform from. Results
   * of the transfrom are saved in the scratch directory as normal.
   */
  class XalanWorker extends Thread implements ErrorListener {

    // Where are we going to get jobs from
    WorkQueue _queue;

    // A unique identifier for the worker
    int _id;

    public XalanWorker(WorkQueue queue, int id) {
      _queue = queue;
      _id = id;
    }

    public void run() {
      try {
        if (verbose)
          System.out.println("Worker thread starting");
        FileOutputStream outputStream = new FileOutputStream(new File(scratch, "xalan.out." + _id));
        Result outFile = new StreamResult(outputStream);
        while (true) {
          String fileName = _queue.pop();
          // An empty string is the end of life signal
          if (fileName.equals(""))
            break;
          Transformer transformer = _template.newTransformer();
          transformer.setErrorListener(this);
          FileInputStream inputStream = new FileInputStream(new File(scratch, fileName));
          Source inFile = new StreamSource(inputStream);
          transformer.transform(inFile, outFile);
          inputStream.close();
        }
      } catch (TransformerConfigurationException e) {
        e.printStackTrace();
      } catch (TransformerException e) {
        e.printStackTrace();
      } catch (IOException e) {
        e.printStackTrace();
      }
      if (verbose)
        System.out.println("Worker thread exiting");
    }

    // Provide an ErrorListener so that stderr warnings can be surpressed
    public void error(TransformerException exception) throws TransformerException {
      throw exception;
    }

    public void fatalError(TransformerException exception) throws TransformerException {
      throw exception;
    }

    public void warning(TransformerException exception) throws TransformerException {
      // Ignore warnings, the test transforms create some
    }
  }

  // The rather inappropriatly named 'Templates' object for storing
  // a handle to a 'compiled' transformation stylesheet
  Templates _template = null;

  // The queue used to hold jobs to be processed
  WorkQueue _workQueue = null;

  // An array for the workers
  XalanWorker[] _workers = null;

  public XSLTBench(File scratch) throws Exception {
    // Check Xalan version, this is easy to get wrong because its
    // bundled with Java these days, so we do explict check
    this.scratch = scratch;
    Properties props = System.getProperties();
    if (!org.apache.xalan.Version.getVersion().equals(XALAN_VERSION)) {
      System.err.println("***  Incorrect version of Xalan in use!");
      System.err.println("***     Should be '" + XALAN_VERSION + "',");
      System.err.println("***     actually is '" + org.apache.xalan.Version.getVersion() + "').");
      System.err.println("***  To fix this, extract the included xalan.jar:");
      System.err.println("***     unzip " + props.get("java.class.path") + " xalan.jar");
      System.err.println("***  and override your jvm's boot classpath:");
      System.err.println("***     java -Xbootclasspath/p:xalan.jar [...] ");
      throw new Exception("Please fix your bootclasspath and try again.");
    }

    // Fix the JAXP transformer to be Xalan
    String key = "javax.xml.transform.TransformerFactory";
    String value = "org.apache.xalan.processor.TransformerFactoryImpl";
    props.put(key, value);
    System.setProperties(props);

    // Compile the test stylesheet for later use
    Source stylesheet = new StreamSource(new File(scratch, "xalan/xmlspec.xsl"));
    TransformerFactory factory = TransformerFactory.newInstance();
    _template = factory.newTemplates(stylesheet);

    // Create the work queue for jobs
    _workQueue = new WorkQueue();

  }

  /**
   * This method is called before the start of a benchmark iteration
   * 
   * @param workers
   */
  public void createWorkers(int workers) {
    this.workers = workers;
    // Setup the workers ready to roll
    if (_workers == null)
      _workers = new XalanWorker[workers];
    for (int i = 0; i < workers; i++) {
      _workers[i] = new XalanWorker(_workQueue, i);
      _workers[i].start();
    }
  }

  /**
   * This method is the heart of a benchmark iteration
   * 
   * @param nRuns
   * @throws InterruptedException
   */
  public void doWork(int nRuns) throws InterruptedException {
    // Post the work
    for (int iRun = 0; iRun < nRuns; iRun++) {
      _workQueue.push("xalan/acks.xml");
      _workQueue.push("xalan/binding.xml");
      _workQueue.push("xalan/changes.xml");
      _workQueue.push("xalan/concepts.xml");
      _workQueue.push("xalan/controls.xml");
      _workQueue.push("xalan/datatypes.xml");
      _workQueue.push("xalan/expr.xml");
      _workQueue.push("xalan/intro.xml");
      _workQueue.push("xalan/model.xml");
      _workQueue.push("xalan/prod-notes.xml");
      _workQueue.push("xalan/references.xml");
      _workQueue.push("xalan/rpm.xml");
      _workQueue.push("xalan/schema.xml");
      _workQueue.push("xalan/structure.xml");
      _workQueue.push("xalan/template.xml");
      _workQueue.push("xalan/terms.xml");
      _workQueue.push("xalan/ui.xml");
    }

    // Kill workers and wait for death
    for (int i = 0; i < workers; i++) {
      _workQueue.push(""); // "" is a thread die signal
    }
    for (int i = 0; i < workers; i++) {
      if (verbose)
        System.out.println("Waiting for thread " + i);
      _workers[i].join();
    }
  }
}
