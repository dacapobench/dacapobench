package dacapo.xalan;

import java.io.*;
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

import dacapo.Benchmark;
import dacapo.DacapoException;
import dacapo.parser.Config;

/*
 * Xalan test harness. Uses a single pre-compiled stylesheet to transfrom
 * a number of sample files using a number of threads. The goal is to 
 * simulate a typical server XSLT load which is performing XML to (X)HTML
 * transforms as part of a presentation layer.
 */
public class XalanHarness extends Benchmark {
	
	// What version of XALAN should we have
	final String XALAN_VERSION = "Xalan Java 2.4.1";
	
	// How may workers do we want
	final int WORKERS = 8;

	/*
	 * A simple queue of filenames that the worker threads pull jobs
	 * from. 
	 */
	class WorkQueue {
		LinkedList _queue = new LinkedList();

		public synchronized void push(String filename) {
			_queue.add(filename);
			notify();
		}

		public synchronized String pop() {
			while (_queue.isEmpty()) {
				try {
					wait();
				} catch (InterruptedException e) {
				}
			}
			return (String) _queue.removeFirst();
		}
	}

	/*
	 * Worker thread. Provided with a queue that input files
	 * can be selected from and a template object that can
	 * be used to perform a transform from. Results of the
	 * transfrom are saved in the scratch directory as normal.
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
        FileOutputStream outputStream = new FileOutputStream(
            new File(scratch,"xalan.out." + _id));
        Result outFile = new StreamResult(outputStream);
				while (true) {
					String fileName = _queue.pop();
					// An empty string is the end of life signal
					if (fileName.equals(""))
						break;
					Transformer transformer = _template.newTransformer();
					transformer.setErrorListener(this);
					FileInputStream inputStream = new FileInputStream(
                        new File(scratch,fileName));
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
		}

		// Provide an ErrorListener so that stderr warnings can be surpressed
		public void error(TransformerException exception) throws TransformerException {
			throw exception;
		}

		public void fatalError(TransformerException exception) throws TransformerException {
			throw exception;
		}

		public void warning(TransformerException exception)	throws TransformerException {
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
	
	public XalanHarness(Config config, File scratch) throws Exception {
    super(config, scratch);

    // Check Xalan version, this is easy to get wrong because its
    // bundled with Java these days, so we do explict check
    Properties props = System.getProperties();
    if (!org.apache.xalan.Version.getVersion().equals(XALAN_VERSION)) {
      System.err.println("***  Incorrect version of Xalan in use!");
      System.err.println("***     Should be '" + XALAN_VERSION + "',");
      System.err.println("***     actually is '" + org.apache.xalan.Version.getVersion() + "').");
      System.err.println("***  To fix this, extract the included xalan.jar:");
      System.err.println("***     unzip "+props.get("java.class.path")+" xalan.jar");
      System.err.println("***  and override your jvm's boot classpath:");
      System.err.println("***     java -Xbootclasspath/p:xalan.jar [...] ");
      throw new DacapoException("Please fix your bootclasspath and try again.");
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
	
	/*
   * Create the threads, this is outside the timing loop to minimise the impact
   * of the startup. The threads will just sit waiting on the work queue.
   * 
   * @see dacapo.Benchmark#preIteration(java.lang.String)
   */
	public void preIteration(String size) throws Exception {
		super.preIteration(size);
		
	   	// Setup the workers ready to roll
		if (_workers==null)
			_workers=new XalanWorker [WORKERS];
	  	for (int i=0; i<WORKERS; i++) {
	  		_workers[i]=new XalanWorker(_workQueue,i);
	  		_workers[i].start();
	  	}
	}

	/*
	 * Run the benchmark by just pushing jobs onto the
	 * work queue and waiting for the threads to finish.
	 * @see dacapo.Benchmark#iterate(java.lang.String)
	 */
	public void iterate(String size) throws Exception {
		String[] harnessArgs = config.getArgs(size);
		int nRuns = Integer.parseInt(harnessArgs[0]);

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
    for (int i = 0; i < WORKERS; i++) {
      _workQueue.push(""); // "" is a thread die signal
    }
    for (int i = 0; i < WORKERS; i++) {
      _workers[i].join();
    }
    System.out.println("Normal completion.");
	}
}
