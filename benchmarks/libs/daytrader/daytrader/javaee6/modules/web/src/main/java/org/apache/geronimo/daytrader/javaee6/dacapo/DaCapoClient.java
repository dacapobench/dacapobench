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

public class DaCapoClient {
	int completed;

	public static void main(String[] args) {
		boolean initialize = false;
		boolean beans = false;
		int logNumSessions = 3;
		try {
			for (int i = 0; i < args.length; i++) {
				if ("-i".equals(args[i])) {
					initialize = true;
				} else if ("-s".equals(args[i])) {
					logNumSessions = Integer.parseInt(args[++i]);
				} else if ("-b".equals(args[i])) {
					beans = true;
				}
			}
			if (initialize) {
				DaCapoTrader.initializeTrade(logNumSessions);
			} else {
				DaCapoRunner.runDaCapoTrade(logNumSessions, !beans);
			}
		} catch (Exception e) {
			System.err.println("Caught an unexpected exception!");
			e.printStackTrace();
		}
	}
}