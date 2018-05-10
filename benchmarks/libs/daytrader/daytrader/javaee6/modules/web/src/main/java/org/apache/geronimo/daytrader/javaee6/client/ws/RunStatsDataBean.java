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

public class RunStatsDataBean  {
    private int tradeUserCount;
    private int newUserCount;
    private int sumLoginCount;
    private int sumLogoutCount;
    private int holdingCount;
    private int buyOrderCount;
    private int sellOrderCount;
    private int cancelledOrderCount;
    private int openOrderCount;
    private int deletedOrderCount;
    private int orderCount;
    private int tradeStockCount;

    public RunStatsDataBean() {
    }

    public int getTradeUserCount() {
        return tradeUserCount;
    }

    public void setTradeUserCount(int tradeUserCount) {
        this.tradeUserCount = tradeUserCount;
    }

    public int getNewUserCount() {
        return newUserCount;
    }

    public void setNewUserCount(int newUserCount) {
        this.newUserCount = newUserCount;
    }

    public int getSumLoginCount() {
        return sumLoginCount;
    }

    public void setSumLoginCount(int sumLoginCount) {
        this.sumLoginCount = sumLoginCount;
    }

    public int getSumLogoutCount() {
        return sumLogoutCount;
    }

    public void setSumLogoutCount(int sumLogoutCount) {
        this.sumLogoutCount = sumLogoutCount;
    }

    public int getHoldingCount() {
        return holdingCount;
    }

    public void setHoldingCount(int holdingCount) {
        this.holdingCount = holdingCount;
    }

    public int getBuyOrderCount() {
        return buyOrderCount;
    }

    public void setBuyOrderCount(int buyOrderCount) {
        this.buyOrderCount = buyOrderCount;
    }

    public int getSellOrderCount() {
        return sellOrderCount;
    }

    public void setSellOrderCount(int sellOrderCount) {
        this.sellOrderCount = sellOrderCount;
    }

    public int getCancelledOrderCount() {
        return cancelledOrderCount;
    }

    public void setCancelledOrderCount(int cancelledOrderCount) {
        this.cancelledOrderCount = cancelledOrderCount;
    }

    public int getOpenOrderCount() {
        return openOrderCount;
    }

    public void setOpenOrderCount(int openOrderCount) {
        this.openOrderCount = openOrderCount;
    }

    public int getDeletedOrderCount() {
        return deletedOrderCount;
    }

    public void setDeletedOrderCount(int deletedOrderCount) {
        this.deletedOrderCount = deletedOrderCount;
    }

    public int getOrderCount() {
        return orderCount;
    }

    public void setOrderCount(int orderCount) {
        this.orderCount = orderCount;
    }

    public int getTradeStockCount() {
        return tradeStockCount;
    }

    public void setTradeStockCount(int tradeStockCount) {
        this.tradeStockCount = tradeStockCount;
    }

}
