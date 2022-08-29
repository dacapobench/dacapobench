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
package org.apache.geronimo.daytrader.javaee6.dacapo;

public class DaCapoRunner {
	
  private static final boolean VERBOSE = false;
  
  static int completed[] = new int[2];
  static int tradeSessions = 0;
  static boolean running = false;
  static int numThreads;

	public static void runDaCapoTrade(int logNumSessions, boolean soap) {
		synchronized(completed) {
			if (!running) {
				running = true;
				numThreads = Integer.parseInt(System.getProperty("dacapo.client.threads"));
				startTraders(logNumSessions, numThreads, soap);
			}
		}
		wait(numThreads);
		running = false;
	}
	
	private static void startTraders(int logNumSessions, int numThreads, boolean soap) {
		LatencyReporter.initialize(numThreads, logNumSessions);
		DaCapoTrader[] traders = new DaCapoTrader[numThreads];
		for (int i = 0; i < numThreads; i++) {
			try {
				traders[i] = new DaCapoTrader(soap, completed, i, numThreads, logNumSessions);
				traders[i].setName("DaCapo Thread " + i);
				if (i == 0) {
					tradeSessions = traders[i].loadWorkload(logNumSessions);
					DaCapoTrader.setSessionStride(tradeSessions, numThreads);
				}
			} catch (Exception e) {
				System.err.println("Caught exception!: "+e.toString());
				System.exit(-1);
			}
		}
		if (tradeSessions > 0) {
			completed[0] = 0;
			for (int i = 0; i < numThreads; i++) {
				traders[i].start();
			}
		}
	}

	private static void wait(int numTraders) {
		synchronized(completed) {
			while (completed[0] != (tradeSessions+numTraders)) {
				try {
					completed.wait(2000);
					if (VERBOSE) System.err.println("Completed: "+completed[0]+"/"+tradeSessions);
				} catch (InterruptedException e) {
					System.out.println("Caught exception while waiting "+ e.toString());
					System.exit(-1);
				}
			}
		}
	}
}