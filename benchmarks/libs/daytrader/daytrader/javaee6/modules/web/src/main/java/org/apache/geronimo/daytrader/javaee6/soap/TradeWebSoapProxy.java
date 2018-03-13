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

import org.apache.geronimo.daytrader.javaee6.*;

import java.util.*;
import java.net.*;
import javax.xml.rpc.*;
import javax.naming.*;
import javax.xml.namespace.*;

import java.rmi.RemoteException;
import java.math.BigDecimal;
import javax.ejb.FinderException;

import org.apache.geronimo.daytrader.javaee6.utils.*;
import org.apache.geronimo.daytrader.javaee6.entities.*;
import org.apache.geronimo.daytrader.javaee6.core.beans.*;

public class TradeWebSoapProxy implements org.apache.geronimo.daytrader.javaee6.core.api.TradeServices {

	private static String servicePort;
	private static org.apache.geronimo.daytrader.javaee6.client.ws.TradeWSServices trade;
	private static final String jndiName = "java:comp/env/service/Trade";

    public TradeWebSoapProxy() {
    	if (trade == null) {
    		trade = getTrade();
    	}
    }

	public static org.apache.geronimo.daytrader.javaee6.client.ws.TradeWSServices getTrade() {
		try {
			trade = getPortFromFactory();
			((Stub)trade)._setProperty("javax.xml.rpc.service.endpoint.address", TradeConfig.getSoapURL());
		}
		catch (Exception e) {
			System.out.println("problem getting trade port " + e);
			e.printStackTrace();
		}
		return trade;
	}

	private static org.apache.geronimo.daytrader.javaee6.client.ws.TradeWSServices getPortFromFactory() throws ServiceException, MalformedURLException {
		if (Log.doTrace()) {
			Log.traceEnter("TradeWebSoapProxt.getPortFromFactory()");
		}
		// JSR 109 lookup
		try {
			InitialContext context = new InitialContext();
			if (Log.doTrace()) {
				Log.trace("attempting JSR109 lookup with jndi of " + jndiName);
			}
			org.apache.geronimo.daytrader.javaee6.client.ws.Trade tradeService1 = (org.apache.geronimo.daytrader.javaee6.client.ws.Trade)context.lookup(jndiName);
			return tradeService1.getTradeWSServices();
		}
		catch (Exception e) {
			Log.error(e, "JSR 109 lookup failed .. defaulting to JSR 101");
		}

		// JSR 101 lookup
		if (Log.doTrace()) {
			Log.trace("attempting JSR101 lookup with url of " + TradeConfig.getSoapURL());
		}
		URL wsdlLoc = new URL(TradeConfig.getSoapURL());
		QName serviceName = new QName("http://daytrader.samples.geronimo.apache.org", "Trade");
		Service tService = ServiceFactory.newInstance().createService(wsdlLoc, serviceName);
		QName portName = new QName("http://daytrader.samples.geronimo.apache.org", "TradeWSServices");
		return (org.apache.geronimo.daytrader.javaee6.client.ws.TradeWSServices)tService.getPort(portName, org.apache.geronimo.daytrader.javaee6.client.ws.TradeWSServices.class);
	}
	
	public static void updateServicePort() {
		// reconstruct Trade as service port has changed
		trade = getTrade();
	}
	
	/* (non-Javadoc)
	 * @see org.apache.geronimo.daytrader.javaee6.TradeServices#buy(java.lang.String, java.lang.String, double, int)
	 */
	public org.apache.geronimo.daytrader.javaee6.entities.OrderDataBean buy(String userID, String symbol, double quantity,	int orderProcessingMode) throws Exception, RemoteException {
		return convertOrderDataBean(getTrade().buy(userID, symbol, quantity, orderProcessingMode));
	}

	/* (non-Javadoc)
	 * @see org.apache.geronimo.daytrader.javaee6.TradeServices#cancelOrder(java.lang.Integer, boolean)
	 */
	public void cancelOrder(Integer orderID, boolean twoPhase) throws Exception, RemoteException {
		getTrade().cancelOrder(orderID, twoPhase);
	}

	/* (non-Javadoc)
	 * @see org.apache.geronimo.daytrader.javaee6.TradeServices#completeOrder(java.lang.Integer, boolean)
	 */
	public OrderDataBean completeOrder(Integer orderID, boolean twoPhase) throws Exception, RemoteException {
		return convertOrderDataBean(getTrade().completeOrder(orderID, twoPhase));
	}

	/* (non-Javadoc)
	 * @see org.apache.geronimo.daytrader.javaee6.TradeServices#createQuote(java.lang.String, java.lang.String, java.math.BigDecimal)
	 */
	public QuoteDataBean createQuote(String symbol, String companyName, BigDecimal price) throws Exception, RemoteException {
		return convertQuoteDataBean(getTrade().createQuote(symbol, companyName, price));
	}

	/* (non-Javadoc)
	 * @see org.apache.geronimo.daytrader.javaee6.TradeServices#getAccountData(java.lang.String)
	 */
	public AccountDataBean getAccountData(String userID) throws FinderException, RemoteException {
		return convertAccountDataBean(getTrade().getAccountData(userID));
	}

	/* (non-Javadoc)
	 * @see org.apache.geronimo.daytrader.javaee6.TradeServices#getAccountProfileData(java.lang.String)
	 */
	public AccountProfileDataBean getAccountProfileData(String userID) throws Exception, RemoteException {
		return convertAccountProfileDataBean(getTrade().getAccountProfileData(userID));
	}

	/* (non-Javadoc)
	 * @see org.apache.geronimo.daytrader.javaee6.TradeServices#getAllQuotes()
	 */
	public Collection getAllQuotes() throws Exception, RemoteException {
		return convertQuoteDataBeanWSArrayToCollectionBase(getTrade().getAllQuotes());
	}

	/* (non-Javadoc)
	 * @see org.apache.geronimo.daytrader.javaee6.TradeServices#getClosedOrders(java.lang.String)
	 */
	public Collection getClosedOrders(String userID) throws Exception, RemoteException {
		Object[] orders = getTrade().getClosedOrders(userID);
		ArrayList ordersRet = new ArrayList();
		if (orders == null || orders.length == 0) {
			return ordersRet;
		}
		for (int ii = 0; ii < orders.length; ii++) {
			ordersRet.add(convertOrderDataBean((org.apache.geronimo.daytrader.javaee6.client.ws.OrderDataBean)orders[ii]));
		}
		return ordersRet;
	}

	/* (non-Javadoc)
	 * @see org.apache.geronimo.daytrader.javaee6.TradeServices#getHolding(java.lang.Integer)
	 */
	public HoldingDataBean getHolding(Integer holdingID) throws Exception, RemoteException {
		return convertHoldingDataBean(getTrade().getHolding(holdingID));
	}

	/* (non-Javadoc)
	 * @see org.apache.geronimo.daytrader.javaee6.TradeServices#getHoldings(java.lang.String)
	 */
	public Collection getHoldings(String userID) throws Exception, RemoteException {
		Object[] holdings = getTrade().getHoldings(userID);
		ArrayList holdingsRet = new ArrayList();
		if (holdings == null || holdings.length == 0) {
			return holdingsRet;
		}
		
		for (int ii = 0; ii < holdings.length; ii++) {
			holdingsRet.add(convertHoldingDataBean((org.apache.geronimo.daytrader.javaee6.client.ws.HoldingDataBean)holdings[ii]));
		}
		return holdingsRet;
	}

	/* (non-Javadoc)
	 * @see org.apache.geronimo.daytrader.javaee6.TradeServices#getMarketSummary()
	 */
	public MarketSummaryDataBean getMarketSummary() throws Exception, RemoteException {
		return convertMarketSummaryDataBean(getTrade().getMarketSummary());
	}

	/* (non-Javadoc)
	 * @see org.apache.geronimo.daytrader.javaee6.TradeServices#getOrders(java.lang.String)
	 */
	public Collection getOrders(String userID) throws Exception, RemoteException {
		Object[] orders = getTrade().getOrders(userID);
		ArrayList ordersRet = new ArrayList();
		if (orders == null || orders.length == 0) {
			return ordersRet;
		}
		for (int ii = 0; ii < orders.length; ii++) {
			ordersRet.add(convertOrderDataBean((org.apache.geronimo.daytrader.javaee6.client.ws.OrderDataBean)orders[ii]));
		}
		return ordersRet;
	}

	/* (non-Javadoc)
	 * @see org.apache.geronimo.daytrader.javaee6.TradeServices#getQuote(java.lang.String)
	 */
	public QuoteDataBean getQuote(String symbol) throws Exception, RemoteException {
		return convertQuoteDataBean(getTrade().getQuote(symbol));
	}

	/* (non-Javadoc)
	 * @see org.apache.geronimo.daytrader.javaee6.TradeServices#login(java.lang.String, java.lang.String)
	 */
	public AccountDataBean login(String userID, String password) throws Exception, RemoteException {
		return convertAccountDataBean(getTrade().login(userID, password));
	}

	/* (non-Javadoc)
	 * @see org.apache.geronimo.daytrader.javaee6.TradeServices#logout(java.lang.String)
	 */
	public void logout(String userID) throws Exception, RemoteException {
		getTrade().logout(userID);
	}

	/* (non-Javadoc)
	 * @see org.apache.geronimo.daytrader.javaee6.TradeServices#orderCompleted(java.lang.String, java.lang.Integer)
	 */
	public void orderCompleted(String userID, Integer orderID) throws Exception, RemoteException {
		getTrade().orderCompleted(userID, orderID);
	}

	/* (non-Javadoc)
	 * @see org.apache.geronimo.daytrader.javaee6.TradeServices#queueOrder(java.lang.Integer, boolean)
	 */
	public void queueOrder(Integer orderID, boolean twoPhase) throws Exception, RemoteException {
		getTrade().queueOrder(orderID, twoPhase);
	}

	/* (non-Javadoc)
	 * @see org.apache.geronimo.daytrader.javaee6.TradeServices#register(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.math.BigDecimal)
	 */
	public AccountDataBean register(String userID, String password, String fullname, String address, String email, String creditcard, BigDecimal openBalance) throws Exception, RemoteException {
		return convertAccountDataBean(getTrade().register(userID, password, fullname, address, email, creditcard, openBalance));
	}

	/* (non-Javadoc)
	 * @see org.apache.geronimo.daytrader.javaee6.TradeServices#resetTrade(boolean)
	 */
	public RunStatsDataBean resetTrade(boolean deleteAll) throws Exception, RemoteException {
		return convertRunStatsDataBean(getTrade().resetTrade(deleteAll));
	}

	/* (non-Javadoc)
	 * @see org.apache.geronimo.daytrader.javaee6.TradeServices#sell(java.lang.String, java.lang.Integer, int)
	 */
	public OrderDataBean sell(String userID, Integer holdingID,	int orderProcessingMode) throws Exception, RemoteException {
		return convertOrderDataBean(getTrade().sell(userID, holdingID, orderProcessingMode));
	}

	/* (non-Javadoc)
	 * @see org.apache.geronimo.daytrader.javaee6.TradeServices#updateAccountProfile(org.apache.geronimo.daytrader.javaee6.entities.AccountProfileDataBean)
	 */
	public AccountProfileDataBean updateAccountProfile(AccountProfileDataBean profileData) throws Exception, RemoteException {
		return convertAccountProfileDataBean(getTrade().updateAccountProfile(convertAccountProfileDataBeanToWS(profileData)));
	}

	/* (non-Javadoc)
	 * @see org.apache.geronimo.daytrader.javaee6.TradeServices#updateQuotePriceVolume(java.lang.String, java.math.BigDecimal, double)
	 */
	public QuoteDataBean updateQuotePriceVolume(String symbol, BigDecimal newPrice, double sharesTraded) throws Exception, RemoteException {
		return convertQuoteDataBean(getTrade().updateQuotePriceVolume(symbol, newPrice, sharesTraded));		
	}

	/**
   * @see TradeServices#runDaCapoTrade(String, int, boolean)
   */
	public void runDaCapoTrade(String size, int threads, boolean soap) throws Exception {
		getTrade().runDaCapoTrade(size, threads, soap);
	}
	
	/**
    * @see TradeServices#initializeDaCapo(String)
   */
	public void initializeDaCapo(String size) throws Exception {
		getTrade().initializeDaCapo(size);
	}

	/**
  * @see TradeServices#resetDaCapo(String, int)
   */
	public boolean resetDaCapo(String size, int threads) throws Exception {
		return getTrade().resetDaCapo(size, threads);
	}
	
	private org.apache.geronimo.daytrader.javaee6.entities.OrderDataBean convertOrderDataBean(org.apache.geronimo.daytrader.javaee6.client.ws.OrderDataBean bean) {
		return new org.apache.geronimo.daytrader.javaee6.entities.OrderDataBean(
			bean.getOrderID(),
			bean.getOrderType(),
			bean.getOrderStatus(),
			bean.getOpenDate() != null ? bean.getOpenDate().getTime() : null,
			bean.getCompletionDate() != null ? bean.getCompletionDate().getTime() : null,
			bean.getQuantity(),
			bean.getPrice(),
			bean.getOrderFee(),
			bean.getSymbol());
	}

	private org.apache.geronimo.daytrader.javaee6.entities.QuoteDataBean convertQuoteDataBean(org.apache.geronimo.daytrader.javaee6.client.ws.QuoteDataBean bean) {
		return new org.apache.geronimo.daytrader.javaee6.entities.QuoteDataBean(
			bean.getSymbol(),
			bean.getCompanyName(),
			bean.getVolume(),
			bean.getPrice(),
			bean.getOpen(),
			bean.getLow(),
			bean.getHigh(),
			bean.getChange());
	}

	private Collection convertQuoteDataBeanWSArrayToCollectionBase(Object[] quotes) {	
		ArrayList quotesRet = new ArrayList();
		if (quotes == null || quotes.length == 0) {
			return quotesRet;
		}
		for (int ii = 0; ii < quotes.length; ii++) {
			quotesRet.add(convertQuoteDataBean((org.apache.geronimo.daytrader.javaee6.client.ws.QuoteDataBean)quotes[ii]));
		}
		return quotesRet;
	}

	private org.apache.geronimo.daytrader.javaee6.entities.HoldingDataBean convertHoldingDataBean(org.apache.geronimo.daytrader.javaee6.client.ws.HoldingDataBean bean) {
		return new org.apache.geronimo.daytrader.javaee6.entities.HoldingDataBean(
			bean.getHoldingID(),
			bean.getQuantity(),
			bean.getPurchasePrice(),
			bean.getPurchaseDate().getTime(),
			bean.getQuoteID());
	}

	private org.apache.geronimo.daytrader.javaee6.entities.AccountDataBean convertAccountDataBean(org.apache.geronimo.daytrader.javaee6.client.ws.AccountDataBean bean) {
		return new org.apache.geronimo.daytrader.javaee6.entities.AccountDataBean(
			bean.getAccountID(),
			bean.getLoginCount(),
			bean.getLogoutCount(),
			bean.getLastLogin().getTime(),
			bean.getCreationDate().getTime(),
			bean.getBalance(),
			bean.getOpenBalance(),
			bean.getProfileID());			
	}

	private org.apache.geronimo.daytrader.javaee6.entities.AccountProfileDataBean convertAccountProfileDataBean(org.apache.geronimo.daytrader.javaee6.client.ws.AccountProfileDataBean bean) {
		return new org.apache.geronimo.daytrader.javaee6.entities.AccountProfileDataBean(
			bean.getUserID(),
			bean.getPassword(),
			bean.getFullName(),
			bean.getAddress(),
			bean.getEmail(),
			bean.getCreditCard());
	}

	private org.apache.geronimo.daytrader.javaee6.client.ws.AccountProfileDataBean convertAccountProfileDataBeanToWS(org.apache.geronimo.daytrader.javaee6.entities.AccountProfileDataBean bean) {
		org.apache.geronimo.daytrader.javaee6.client.ws.AccountProfileDataBean beanRet = new org.apache.geronimo.daytrader.javaee6.client.ws.AccountProfileDataBean();
		beanRet.setUserID(bean.getUserID());
		beanRet.setPassword(bean.getPassword());
		beanRet.setFullName(bean.getFullName());
		beanRet.setAddress(bean.getAddress());
		beanRet.setEmail(bean.getEmail());
		beanRet.setCreditCard(bean.getCreditCard());
		return beanRet;
	}

	private org.apache.geronimo.daytrader.javaee6.core.beans.MarketSummaryDataBean convertMarketSummaryDataBean(org.apache.geronimo.daytrader.javaee6.client.ws.MarketSummaryDataBeanWS bean) {
		org.apache.geronimo.daytrader.javaee6.core.beans.MarketSummaryDataBean retBean = new org.apache.geronimo.daytrader.javaee6.core.beans.MarketSummaryDataBean();
		retBean.setTopGainers(convertQuoteDataBeanWSArrayToCollectionBase(bean.getTopGainers()));
		retBean.setTopLosers(convertQuoteDataBeanWSArrayToCollectionBase(bean.getTopLosers()));
		retBean.setTSIA(bean.getTSIA());
		retBean.setOpenTSIA(bean.getOpenTSIA());
		retBean.setVolume(bean.getVolume());
		// can't use contructor of MSBean as it sets this to the current time
		retBean.setSummaryDate(bean.getSummaryDate().getTime());
		return retBean; 
	}
	
	private org.apache.geronimo.daytrader.javaee6.core.beans.RunStatsDataBean convertRunStatsDataBean(org.apache.geronimo.daytrader.javaee6.client.ws.RunStatsDataBean bean) {
		org.apache.geronimo.daytrader.javaee6.core.beans.RunStatsDataBean beanRet = new org.apache.geronimo.daytrader.javaee6.core.beans.RunStatsDataBean();
		beanRet.setTradeUserCount(bean.getTradeUserCount());
		beanRet.setNewUserCount(bean.getNewUserCount());
		beanRet.setSumLoginCount(bean.getSumLoginCount());
		beanRet.setSumLogoutCount(bean.getSumLogoutCount());
		beanRet.setHoldingCount(bean.getHoldingCount());
		beanRet.setOrderCount(bean.getOrderCount());
		beanRet.setBuyOrderCount(bean.getBuyOrderCount());
		beanRet.setSellOrderCount(bean.getSellOrderCount());
		beanRet.setCancelledOrderCount(bean.getCancelledOrderCount());
		beanRet.setOpenOrderCount(bean.getOpenOrderCount());
		beanRet.setDeletedOrderCount(bean.getDeletedOrderCount());
		return beanRet;
	}
}
