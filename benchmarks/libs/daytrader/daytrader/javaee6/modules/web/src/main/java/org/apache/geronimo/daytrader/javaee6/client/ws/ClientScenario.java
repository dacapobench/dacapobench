/**
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.geronimo.daytrader.javaee6.client.ws;

import java.net.URL;
import javax.naming.*;

/* ServiceFactory */
import javax.xml.rpc.*;
import javax.xml.namespace.QName;

/**
 * Web Services J2EE client for Trade.
 */
public class ClientScenario extends Thread {

	/**
	 * A flag to determine how to get the port.  If true, use JNDI 
	 * lookup to get a Service.
	 * To use a ServiceFactory to obtain a Service, set useJNDI false.
	 * If useJNDI is true, the client must be run in a J2EE client container.
	 */
	protected static final boolean useJNDI = false;

	//Properties pertaining to this thread instance in the scenario
	// Request statistics
	private long numReqs = 0;	
	private long numErrs = 0;
	private long numStatReqs = 0;	 // The number of requests since the last statistics  cdear
	private long totResp = 0;
	private boolean stop = false;

	//Properties pertaining to the entire scenario
    // remove ?wsdl
    private static String servicePort = "http://localhost:8080/daytrader/services/TradeWSServices";
	private static long numThreads = 0; // # threads in scenario
	private static long reqPerThread; // # requests for each thread to process
	private static long startTime = 0; // Time (millis) when the scen. started
	private static long statStartTime = 0;
	// Time (millis) demarking the begin time for the stats window
	private static long intervalStartTime = 0;
	// Time when the last stats interval started

	private static long totReqsAtLastInterval = 0;

	private static long minResp = Long.MAX_VALUE;
	private static long maxResp = 0;

	private static final String jndiName = "java:comp/env/service/Trade";
	
	public static String symbol = "s:1";

	private static TradeWSServices tradeSingleton = null;

	public ClientScenario() {
	}
	
	public ClientScenario(int reqPerThreadIn) {
		System.out.println(
			"Thread "
				+ this.getName()
				+ " ready to execute "
				+ reqPerThread
				+ " iterations");
		reqPerThread = reqPerThreadIn;
	}

	public void run() {
		TradeWSServices ts;
		try {
			ts = getTrade();
			for (int i = 0; i < reqPerThread; i++) {
				try {
					if (isStop()) {
						System.out.println("Thread " + this +" stopping");
						return;
					}
					//TODO -- scenario and need random quote
					long start = System.currentTimeMillis();

					//performScenario 
					QuoteDataBean resp = ts.getQuote(symbol);

					//update Statistics		
					long end = System.currentTimeMillis();
					long respTime = end - start;
					totResp += respTime;
					numReqs++;
					numStatReqs++;
					if ((respTime < minResp))
						setMinResp(respTime);
					if (respTime > maxResp)
						setMaxResp(respTime);
				} catch (Exception e) {
					System.out.println("Thread error -- scenario = xxx" + e.toString());
					e.printStackTrace();
					numErrs++;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected long clearStats() {
		maxResp = totResp = 0;
		minResp = Long.MAX_VALUE;
		totResp = 0;
		numStatReqs = 0;
		return numReqs;
	}

	/**
	 * Get a web services port that represents the Trade services.
	 * First try JSR 109 lookup, then fall back on error to JSR 101. 
	 *
	 * @return Trade Services Interface 
	 * @exception javax.xml.rpc.ServiceException if accessing the service or
	 *			port fails.
	 * @exception java.net.MalformedURLException if an invalid URL is obtained.
	 */
	protected static TradeWSServices getTrade() throws javax.xml.rpc.ServiceException, java.net.MalformedURLException {
		TradeWSServices trade;

		// JSR 109 lookup
		try {
			InitialContext context = new InitialContext();
			
			Trade tradeService1 = (Trade)context.lookup(jndiName);
			trade = tradeService1.getTradeWSServices();
		}
		catch (Exception e) {
			System.out.println("JSR 109 lookup failed .. defaulting to JSR 101");
			// JSR 101 lookup
			URL wsdlLoc = new URL(getServicePort());
			QName serviceName = new QName("http://daytrader.samples.geronimo.apache.org", "Trade");
			Service tService = ServiceFactory.newInstance().createService(wsdlLoc, serviceName);
			QName portName = new QName("http://daytrader.samples.geronimo.apache.org", "TradeWSServices");
			trade = (TradeWSServices)tService.getPort(portName, TradeWSServices.class);
		}
		((Stub)trade)._setProperty("javax.xml.rpc.service.endpoint.address", getServicePort());
		return trade;
	}

	public static TradeWSServices getTradeSingleton() throws Exception {
		if (tradeSingleton == null) {
			tradeSingleton = getTrade();
		}
		return tradeSingleton;
	}

	/**
	 * Returns the intervalStartTime.
	 * @return long
	 */
	public static long getIntervalStartTime() {
		return intervalStartTime;
	}

	/**
	 * Returns the maxResp.
	 * @return long
	 */
	public static long getMaxResp() {
		return maxResp;
	}

	/**
	 * Returns the minResp.
	 * @return long
	 */
	public static long getMinResp() {
		return minResp;
	}

	/**
	 * Returns the numThreads.
	 * @return long
	 */
	public static long getNumThreads() {
		return numThreads;
	}

	/**
	 * Returns the reqPerThread.
	 * @return long
	 */
	public static long getReqPerThread() {
		return reqPerThread;
	}

	/**
	 * Returns the startTime.
	 * @return long
	 */
	public static long getStartTime() {
		return startTime;
	}

	/**
	 * Returns the statStartTime.
	 * @return long
	 */
	public static long getStatStartTime() {
		return statStartTime;
	}

	/**
	 * Returns the totReqsAtLastInterval.
	 * @return long
	 */
	public static long getTotReqsAtLastInterval() {
		return totReqsAtLastInterval;
	}

	/**
	 * Returns the numErrs.
	 * @return long
	 */
	public long getNumErrs() {
		return numErrs;
	}

	/**
	 * Returns the numReqs.
	 * @return long
	 */
	public long getNumReqs() {
		return numReqs;
	}

	/**
	 * Returns the stop.
	 * @return boolean
	 */
	public boolean isStop() {
		return stop;
	}

	/**
	 * Returns the totResp.
	 * @return long
	 */
	public long getTotResp() {
		return totResp;
	}

	/**
	 * Sets the intervalStartTime.
	 * @param intervalStartTime The intervalStartTime to set
	 */
	public static void setIntervalStartTime(long intervalStartTime) {
		ClientScenario.intervalStartTime = intervalStartTime;
	}

	/**
	 * Sets the maxResp.
	 * @param maxResp The maxResp to set
	 */
	public synchronized static void setMaxResp(long maxResp) {
		ClientScenario.maxResp = maxResp;
	}

	/**
	 * Sets the minResp.
	 * @param minResp The minResp to set
	 */
	public synchronized static void setMinResp(long minResp) {
		if (minResp > 0 )
			ClientScenario.minResp = minResp;
	}

	/**
	 * Sets the numThreads.
	 * @param numThreads The numThreads to set
	 */
	public static void setNumThreads(long numThreads) {
		ClientScenario.numThreads = numThreads;
	}

	/**
	 * Sets the reqPerThread.
	 * @param reqPerThread The reqPerThread to set
	 */
	public static void setReqPerThread(long reqPerThread) {
		ClientScenario.reqPerThread = reqPerThread;
	}

	/**
	 * Sets the startTime.
	 * @param startTime The startTime to set
	 */
	public static void setStartTime(long startTime) {
		ClientScenario.startTime = startTime;
	}

	/**
	 * Sets the statStartTime.
	 * @param statStartTime The statStartTime to set
	 */
	public static void setStatStartTime(long statStartTime) {
		ClientScenario.statStartTime = statStartTime;
	}

	/**
	 * Sets the totReqsAtLastInterval.
	 * @param totReqsAtLastInterval The totReqsAtLastInterval to set
	 */
	public static void setTotReqsAtLastInterval(long totReqsAtLastInterval) {
		ClientScenario.totReqsAtLastInterval = totReqsAtLastInterval;
	}


	/**
	 * Sets the numErrs.
	 * @param numErrs The numErrs to set
	 */
	public void setNumErrs(long numErrs) {
		this.numErrs = numErrs;
	}

	/**
	 * Sets the numReqs.
	 * @param numReqs The numReqs to set
	 */
	public void setNumReqs(long numReqs) {
		this.numReqs = numReqs;
	}

	/**
	 * Sets the stop.
	 * @param stop The stop to set
	 */
	public void setStop(boolean stop) {
		this.stop = stop;
	}

	/**
	 * Sets the totResp.
	 * @param totResp The totResp to set
	 */
	public void setTotResp(long totResp) {
		this.totResp = totResp;
	}

	/**
	 * Returns the totalNumRequests.
	 * @return long
	 */
	public static long getTotalNumRequests() {
		return numThreads * reqPerThread;
	}



	/**
	 * Returns the numStatReqs.
	 * @return long
	 */
	public long getNumStatReqs() {
		return numStatReqs;
	}

	/**
	 * Sets the numStatReqs.
	 * @param numStatReqs The numStatReqs to set
	 */
	public void setNumStatReqs(long numStatReqs) {
		this.numStatReqs = numStatReqs;
	}

	/**
	 * Returns the servicePort.
	 * @return String
	 */
	public static String getServicePort() {
		return servicePort;
	}

	/**
	 * Sets the servicePort.
	 * @param servicePort The servicePort to set
	 */
	public static void setServicePort(String servicePort) {
		ClientScenario.servicePort = servicePort;
		tradeSingleton = null;
	}

}
