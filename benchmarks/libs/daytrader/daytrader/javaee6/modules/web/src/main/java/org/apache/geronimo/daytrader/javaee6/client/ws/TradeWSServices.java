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

public interface TradeWSServices extends java.rmi.Remote {
    public org.apache.geronimo.daytrader.javaee6.client.ws.MarketSummaryDataBeanWS getMarketSummary() throws java.rmi.RemoteException;
    public org.apache.geronimo.daytrader.javaee6.client.ws.OrderDataBean buy(java.lang.String userID, java.lang.String symbol, double quantity, int orderProcessingMode) throws java.rmi.RemoteException;
    public org.apache.geronimo.daytrader.javaee6.client.ws.OrderDataBean sell(java.lang.String userID, java.lang.Integer holdingID, int orderProcessingMode) throws java.rmi.RemoteException;
    public void queueOrder(java.lang.Integer orderID, boolean twoPhase) throws java.rmi.RemoteException;
    public org.apache.geronimo.daytrader.javaee6.client.ws.OrderDataBean completeOrder(java.lang.Integer orderID, boolean twoPhase) throws java.rmi.RemoteException;
    public void cancelOrder(java.lang.Integer orderID, boolean twoPhase) throws java.rmi.RemoteException;
    public void orderCompleted(java.lang.String userID, java.lang.Integer orderID) throws java.rmi.RemoteException;
    public org.apache.geronimo.daytrader.javaee6.client.ws.OrderDataBean[] getOrders(java.lang.String userID) throws java.rmi.RemoteException;
    public org.apache.geronimo.daytrader.javaee6.client.ws.OrderDataBean[] getClosedOrders(java.lang.String userID) throws java.rmi.RemoteException;
    public org.apache.geronimo.daytrader.javaee6.client.ws.QuoteDataBean createQuote(java.lang.String symbol, java.lang.String companyName, java.math.BigDecimal price) throws java.rmi.RemoteException;
    public org.apache.geronimo.daytrader.javaee6.client.ws.QuoteDataBean getQuote(java.lang.String symbol) throws java.rmi.RemoteException;
    public org.apache.geronimo.daytrader.javaee6.client.ws.QuoteDataBean[] getAllQuotes() throws java.rmi.RemoteException;
    public org.apache.geronimo.daytrader.javaee6.client.ws.QuoteDataBean updateQuotePriceVolume(java.lang.String symbol, java.math.BigDecimal newPrice, double sharesTraded) throws java.rmi.RemoteException;
    public org.apache.geronimo.daytrader.javaee6.client.ws.HoldingDataBean[] getHoldings(java.lang.String userID) throws java.rmi.RemoteException;
    public org.apache.geronimo.daytrader.javaee6.client.ws.HoldingDataBean getHolding(java.lang.Integer holdingID) throws java.rmi.RemoteException;
    public org.apache.geronimo.daytrader.javaee6.client.ws.AccountDataBean getAccountData(java.lang.String userID) throws java.rmi.RemoteException;
    public org.apache.geronimo.daytrader.javaee6.client.ws.AccountProfileDataBean getAccountProfileData(java.lang.String userID) throws java.rmi.RemoteException;
    public org.apache.geronimo.daytrader.javaee6.client.ws.AccountProfileDataBean updateAccountProfile(org.apache.geronimo.daytrader.javaee6.client.ws.AccountProfileDataBean profileData) throws java.rmi.RemoteException;
    public org.apache.geronimo.daytrader.javaee6.client.ws.AccountDataBean login(java.lang.String userID, java.lang.String password) throws java.rmi.RemoteException;
    public boolean logout(java.lang.String userID) throws java.rmi.RemoteException;
    public org.apache.geronimo.daytrader.javaee6.client.ws.AccountDataBean register(java.lang.String userID, java.lang.String password, java.lang.String fullname, java.lang.String address, java.lang.String email, java.lang.String creditcard, java.math.BigDecimal openBalance) throws java.rmi.RemoteException;
    public org.apache.geronimo.daytrader.javaee6.client.ws.RunStatsDataBean resetTrade(boolean deleteAll) throws java.rmi.RemoteException;
    public boolean runDaCapoTrade(java.lang.String size, int threads, boolean soap) throws java.rmi.RemoteException;
    public boolean initializeDaCapo(java.lang.String size) throws java.rmi.RemoteException;
    public boolean resetDaCapo(java.lang.String size, int threads) throws java.rmi.RemoteException;
}
