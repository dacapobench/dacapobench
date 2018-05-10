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
package org.apache.geronimo.daytrader.javaee6.soap;

import java.math.BigDecimal;
import java.rmi.RemoteException;

import org.apache.geronimo.daytrader.javaee6.web.TradeAction;
import org.apache.geronimo.daytrader.javaee6.client.ws.AccountDataBean;
import org.apache.geronimo.daytrader.javaee6.client.ws.AccountProfileDataBean;
import org.apache.geronimo.daytrader.javaee6.client.ws.HoldingDataBean;
import org.apache.geronimo.daytrader.javaee6.client.ws.MarketSummaryDataBeanWS;
import org.apache.geronimo.daytrader.javaee6.client.ws.OrderDataBean;
import org.apache.geronimo.daytrader.javaee6.client.ws.QuoteDataBean;
import org.apache.geronimo.daytrader.javaee6.client.ws.RunStatsDataBean;
import org.apache.geronimo.daytrader.javaee6.client.ws.TradeWSServices;

/** 
 * This is a TradeAction wrapper to handle web service handling
 * of collections.  Instead this class uses typed arrays.
 */
public class TradeWSAction implements TradeWSServices {
	TradeAction trade;
	
	public TradeWSAction() {
		trade = new TradeAction();
	}

	public MarketSummaryDataBeanWS getMarketSummary() throws RemoteException {
		try {
            return Convert.convertMarketSummaryDataBean(trade.getMarketSummary());
        } catch (Exception e) {
            throw new RemoteException("", e);
        }
	}
	
	public OrderDataBean buy(String userID, String symbol, double quantity, int orderProcessingMode) throws RemoteException {
		try {
            return Convert.convertOrderDataBean(trade.buy(userID, symbol, quantity, orderProcessingMode));
        } catch (Exception e) {
            throw new RemoteException("", e);
        }
	}
	
	public OrderDataBean sell(String userID, Integer holdingID, int orderProcessingMode) throws RemoteException {
		try {
            return Convert.convertOrderDataBean(trade.sell(userID, holdingID, orderProcessingMode));
        } catch (Exception e) {
            throw new RemoteException("", e);
        }
	}

	public void queueOrder(Integer orderID, boolean twoPhase) throws RemoteException {
		trade.queueOrder(orderID, twoPhase);
	}
	
	public OrderDataBean completeOrder(Integer orderID, boolean twoPhase) throws RemoteException {
		return Convert.convertOrderDataBean(trade.completeOrder(orderID, twoPhase));
	}

	public void cancelOrder(Integer orderID, boolean twoPhase) throws RemoteException {
		trade.cancelOrder(orderID, twoPhase);
	}
	
	public void orderCompleted(String userID, Integer orderID) throws RemoteException {
		try {
            trade.orderCompleted(userID, orderID);
        } catch (Exception e) {
            throw new RemoteException("", e);
        }
	}
	
	public OrderDataBean[] getOrders(String userID) throws RemoteException {
        try {
            return Convert.convertOrderDataBeanCollection(trade.getOrders(userID));
        } catch (Exception e) {
            throw new RemoteException("", e);
        }
	}
	
	public OrderDataBean[] getClosedOrders(String userID) throws RemoteException {
        try {
            return Convert.convertOrderDataBeanCollection(trade.getClosedOrders(userID));
        } catch (Exception e) {
            throw new RemoteException("", e);
        }
	}
	
	public QuoteDataBean createQuote(String symbol, String companyName, BigDecimal price) throws RemoteException {
		try {
            return Convert.convertQuoteDataBean(trade.createQuote(symbol, companyName, price));
        } catch (Exception e) {
            throw new RemoteException("", e);
        }
	}
	
	public QuoteDataBean getQuote(String symbol) throws RemoteException {
		try {
            return Convert.convertQuoteDataBean(trade.getQuote(symbol));
        } catch (Exception e) {
            throw new RemoteException("", e);
        }
	}
	
	public QuoteDataBean[] getAllQuotes() throws RemoteException {
		try {
            return Convert.convertQuoteDataBeanCollection(trade.getAllQuotes());
        } catch (Exception e) {
            throw new RemoteException("", e);
        }
	}
	
	public QuoteDataBean updateQuotePriceVolume(String symbol, BigDecimal newPrice, double sharesTraded) throws RemoteException {
		try {
            return Convert.convertQuoteDataBean(trade.updateQuotePriceVolume(symbol, newPrice, sharesTraded));
        } catch (Exception e) {
            throw new RemoteException("", e);
        }
	}
	
	public HoldingDataBean[] getHoldings(String userID) throws RemoteException {
		try {
            return Convert.convertHoldingDataBeanCollection(trade.getHoldings(userID));
        } catch (Exception e) {
            throw new RemoteException("", e);
        }
	}
	
	public HoldingDataBean getHolding(Integer holdingID) throws RemoteException {
		try {
            return Convert.convertHoldingDataBean(trade.getHolding(holdingID));
        } catch (Exception e) {
            throw new RemoteException("", e);
        }
	}
	
	public AccountDataBean getAccountData(String userID) throws RemoteException {
		try {
            return Convert.convertAccountDataBean(trade.getAccountData(userID));
        } catch (Exception e) {
            throw new RemoteException("", e);
        }
	}
	
	public AccountProfileDataBean getAccountProfileData(String userID) throws RemoteException {
		try {
            return Convert.convertAccountProfileDataBean(trade.getAccountProfileData(userID));
        } catch (Exception e) {
            throw new RemoteException("", e);
        }
	}
	
	public AccountProfileDataBean updateAccountProfile(AccountProfileDataBean profileData) throws RemoteException {
		try {
            return Convert.convertAccountProfileDataBean(trade.updateAccountProfile(Convert.convertAccountProfileDataBean(profileData)));
        } catch (Exception e) {
            throw new RemoteException("", e);            
        }
	}
	
	public AccountDataBean login(String userID, String password) throws RemoteException {
		try {
            return Convert.convertAccountDataBean(trade.login(userID, password));
        } catch (Exception e) {
            throw new RemoteException("", e);
        }
	}
	
	public boolean logout(String userID) throws RemoteException {
		try {
            trade.logout(userID);
            return true;
        } catch (Exception e) {
            throw new RemoteException("", e);
        }
	}
	
	public AccountDataBean register(String userID, String password, String fullname, String address, String email, String creditcard, BigDecimal openBalance) throws RemoteException {
		try {
            return Convert.convertAccountDataBean(trade.register(userID, password, fullname, address, email, creditcard, openBalance));
        } catch (Exception e) {
            throw new RemoteException("", e);
        }
	}
	
	public RunStatsDataBean resetTrade(boolean deleteAll) throws RemoteException {
		try {
            return Convert.convertRunStatsDataBean(trade.resetTrade(deleteAll));
        } catch (Exception e) {
            throw new RemoteException("", e);
        }                
	}    
    
	public boolean runDaCapoTrade(String size, int threads, boolean soap) throws RemoteException {
		try {
            trade.runDaCapoTrade(size, threads, soap);
            return true;
        } catch (Exception e) {
            throw new RemoteException("", e);
        }
	}
		public boolean initializeDaCapo(String size) throws RemoteException {
		try {
            trade.initializeDaCapo(size);
            return true;
        } catch (Exception e) {
            throw new RemoteException("", e);
        }
	}
	
	public boolean resetDaCapo(String size, int threads) throws RemoteException {
		try {
            return trade.resetDaCapo(size, threads);
        } catch (Exception e) {
            throw new RemoteException("", e);
        }
	}
}
