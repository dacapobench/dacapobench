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

import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

import org.apache.geronimo.daytrader.javaee6.core.api.*;
import org.apache.geronimo.daytrader.javaee6.core.direct.*;
import org.apache.geronimo.daytrader.javaee6.entities.*;
import org.apache.geronimo.daytrader.javaee6.utils.*;

public class DaCapoDBBuilder extends Thread {

	static final String maker = "";

	static final boolean VERBOSE = false;
	
	static int numBuilders;
	static int threadCount = 0;
	static int userCount = 0;
	static String[] users = null;
	static String[] stocks = null;
	private static final int MAX_TX_ATTEMPTS = 5;

	int units;
	int ordinal;
	TradeServices trade;

	static {

	}
	
	public static void create(TradeJEEDirect trade, int numThreads) {
		try	{
			createDB(trade);
		} catch (Exception e) {
			System.err.println("Could not create database: "+e.toString());
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	private static void resetDataBase(TradeServices trade) {
		try	{
			trade.resetTrade(true);
		} catch (Exception e) {
			System.err.println("Could not reset database: "+e.toString());
			e.printStackTrace();
			System.exit(-1);
		}
	}

	synchronized private static void readStrings(int logNumSessions) {
		if (users == null || stocks == null) {
			users = readStrings("users.txt", logNumSessions);
			stocks = readStrings("stocks.txt", logNumSessions);
		}
	}

	synchronized private static int getOrdinal(TradeServices trade) {
		try {
			int ordinal = threadCount++;
			if (VERBOSE) System.err.println("Thread " + ordinal + " resetting!");
			if (ordinal == 0) {
				System.out.println(maker + "Resetting database and populating with " + stocks.length + " stocks...");
				resetDataBase(trade);
				populateStocks(trade);
				System.out.println(maker + "Populating database with " + users.length + " users...");
				if (VERBOSE) System.err.println("done with global reset!");
			}
			return ordinal;
		}catch (Exception e){
			System.out.println("It failed");
			System.exit(-1);
		}
		return 1;
	}

	
	public static boolean reset(TradeServices trade, int logNumSessions, int threads) {
		readStrings(logNumSessions);
		int ordinal = getOrdinal(trade);
		populateUsers(trade, ordinal, threads);
		if (ordinal == threads - 1) {
			threadCount = 0; // all done
	//		userCount = 0;
		}
		return true;
	}
	
	private static void populateStocks(TradeServices trade) {
		for (int i = 0; i < stocks.length; i++) {
			String s = stocks[i];
			s = s.trim();
			if ((s.length() != 0) && (s.charAt(0) != '#')) { // Empty lines or lines starting with "#" are ignored
				String[] str = s.split("\t");
				String symbol = str[0];
				String company = str[1];
				java.math.BigDecimal quote = new java.math.BigDecimal(str[2]);
				for (int j = 0; j < MAX_TX_ATTEMPTS; j++) {
					try {
						QuoteDataBean quoteData = trade.createQuote(symbol, company, quote);
						break;
					} catch (Exception e) {
						if (j == MAX_TX_ATTEMPTS - 1) {
							System.err.println("Error adding quote: "+e.toString());
							e.printStackTrace();
							System.exit(-1);
						}
					}
				}
				if (VERBOSE && i % 100 == 0) {
					System.err.print("["+symbol+"|"+company+"|"+quote+"]");
				}
			}
		}
		if (VERBOSE) System.err.println("Hooray, added "+stocks.length+" stocks.");
	}

	private static void populateUsers(TradeServices trade, int ordinal, int threads) {
		String user;
		while ((user = getNextUser(users, ordinal, threads)) != null) {
			addUser(trade, user);
		}
	}
	
	private static int getNumStrings(URL inputFile, String size, boolean stocks) {
		int rtn = 0;
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(inputFile.openStream()));
			if (stocks) {
				String s;
				while ((s = br.readLine()) != null)  {
					s = s.trim();
					if ((s.length() != 0) && (s.charAt(0) != '#')) { // Empty lines or lines starting with "#" are ignored
						rtn++;
					}
				}
			} else {
				rtn = parseUserHeader(br.readLine().trim(), size);
			}
		} catch (Exception e) {
			System.err.println("Caught exception while trying to establish size of input file: "+e.toString());
			e.printStackTrace();
			System.exit(-1);			
		}
		return rtn;
	}
	
	private static String[] readStrings(String fileName, int logNumSessions) {
		URL inputFile = null;
		try {
			inputFile = getURL(fileName); 
			if (inputFile == null) {
				String msg = "DaCapoDBBuilder: user input file doesnt exist at path "+ fileName +" , please provide the file and retry";
				Log.error(msg);
				System.err.println(msg);
				return null;
			}

			int numStrings = getNumStrings(inputFile, "s"+logNumSessions, fileName.equalsIgnoreCase("stocks.txt"));
			if (numStrings == 0) {
				String msg = "DaCapoDBBuilder: can't determine the number of users for size \""+logNumSessions+"\" in input file "+ fileName +" , please provide the file and retry";
				Log.error(msg);
				System.err.println(msg);
				return null;
			}

			String[] strings = new String[numStrings];
			String s;
			int i = 0;
			BufferedReader br = new BufferedReader(new InputStreamReader(inputFile.openStream()));
			while (i < numStrings && (s = br.readLine().trim()) != null)  {
				if ((s.length() != 0) && (s.charAt(0) != '#')){
					strings[i] = s;
					i++;
				}
			}
			return strings;
		} catch (Exception e) {
			System.err.println("Caught exception while trying to read user stocks: "+e.toString());
			e.printStackTrace();
			System.exit(-1);
		}
		return null;
	}
	
	private synchronized static String getNextUser(String[] users, int ordinal, int threads) {
		int user = userCount++;
		if (user < users.length) {
			if (VERBOSE) System.err.println("Thread "+ordinal+" got user "+user+" of "+users.length+"!");
			return users[user];
		} else {
			if (user == (users.length + threads - 1))
				userCount = 0;
			return null;
		}
	}	
	
	
	private static void addUser(TradeServices trade, String userString) {
		String[] str = userString.split("\t");
		String userid = str[0];
		java.math.BigDecimal balance = new java.math.BigDecimal(str[1]);
		String creditcard = str[2];
		String email = str[3];
		String password = str[4];
		String name = str[5];
		String address = str[6];
		for (int i = 0; i < MAX_TX_ATTEMPTS; i++) {
			try {
				AccountDataBean accountData =	trade.register(userid, password, name, address, email, creditcard, balance);
				break;
			} catch (Exception e) {
				if (i == MAX_TX_ATTEMPTS - 1) {
					System.err.println("Error adding account: "+e.toString());
					e.printStackTrace();
					return;
				}
			}
		}

		String[] holdings = str[7].split(", ");
		for (int h = 0; h < holdings.length; h++) {
			String[] stock = holdings[h].split(" ");
			String symbol = stock[0];
			int quantity = Integer.parseInt(stock[1]);
			for (int i = 0; i < MAX_TX_ATTEMPTS; i++) {
				try {
					OrderDataBean orderData = trade.buy(userid, symbol, quantity, TradeConfig.SYNCH);
				} catch (Exception e) {
					if (i == MAX_TX_ATTEMPTS - 1) {
						System.err.println("Error adding holding: "+e.toString());
						e.printStackTrace();
						System.exit(-1);
					}
				}
			}
		}
	}

	
	private static int parseUserHeader(String s, String size) {
		int users = 0;
		String[] header = s.split("\t");
		for (int h = 0; h < header.length; h++) {
			if (header[h].indexOf(size) != -1) {
				try {
					users = Integer.parseInt(header[h].substring(header[h].indexOf(": ")+2));
				} catch (NumberFormatException e) {
					String msg = "DaCapoDBBuilder: garbled size field in user header: "+header[h];
					Log.error(msg);
					System.err.println(msg);
					System.exit(-1);
				}
			}
		}
		return users;
	}
	
	private static void createDB(TradeJEEDirect trade) {
		URL ddlFile = null;
		Object[] sqlBuffer = null;
		try {
			String name = "dbscripts/other/Table.ddl";
			ddlFile = getURL(name);
			if (ddlFile == null) {
				String msg = "DaCapoDBBuilder: DDL file doesnt exist at path "+ name +" , please provide the file and retry";
				Log.error(msg);
				System.err.println(msg);
				return;
			}
			try {
				sqlBuffer = parseDDLToBuffer(ddlFile);
			} catch (Exception e) {
				System.exit(-1);
			}
			try {
				if (trade.recreateDBTables(sqlBuffer, null)) {
					System.out.println(maker + "Successfully created tables");
				}
			} catch (Exception e) {
				String msg = "Unable to drop and recreate DayTrader Db Tables, please check for database consistency before continuing";
				Log.error(msg);
				System.err.println(msg);
				System.exit(-1);
			}
		} catch (Exception e) {
			String msg = "DaCapoDBBuilder: Unable to create database";
			Log.error(msg);
			System.err.println(msg);
			System.exit(-1);
		}
	}

	public DaCapoDBBuilder(int ordinal, int units) {
		this.ordinal = ordinal;
		this.units = units;
		this.trade = new TradeJEEDirect();
	}

	public static Object[] parseDDLToBuffer(URL ddlFile) throws Exception	{
		BufferedReader br = null;
		ArrayList sqlBuffer = new ArrayList(30); //initial capacity 30 assuming we have 30 ddl-sql statements to read

		try {
			if (Log.doTrace()) Log.traceEnter("TradeBuildDB:parseDDLToBuffer - " + ddlFile);

			br = new BufferedReader(new InputStreamReader(ddlFile.openStream()));
			String s;
			String sql = new String();
			while ((s = br.readLine()) != null)  {
				s = s.trim();
				if ((s.length() != 0) && (s.charAt(0) != '#')) { // Empty lines or lines starting with "#" are ignored
					sql = sql +" "+ s;
					if (s.endsWith(";")) { // reached end of sql statement
						sql = sql.replace(';', ' '); //remove the semicolon
						//System.out.println (sql);
						sqlBuffer.add(sql);
						sql = "";
					}
				}
			}
		} catch (IOException ex) {
			Log.error("DaCapoDBBuilder:parseDDLToBuffer Exeception during open/read of File: " + ddlFile, ex);
			throw ex;
		}	finally	{
			if (br != null) {
				try {
					br.close();
				} catch (IOException ex) {
					Log.error("TradeBuildDB:parseDDLToBuffer Failed to close BufferedReader", ex);
					System.exit(-1);
				}
			}
		}
		return sqlBuffer.toArray();
	}

  /**
   * Translate a resource name into a URL.
   *
   * @param fn
   * @return
   */
  public static URL getURL(String fn) {
    ClassLoader cl = DaCapoDBBuilder.class.getClassLoader();
    URL resource = cl.getResource(fn);
    return resource;
  }
}
