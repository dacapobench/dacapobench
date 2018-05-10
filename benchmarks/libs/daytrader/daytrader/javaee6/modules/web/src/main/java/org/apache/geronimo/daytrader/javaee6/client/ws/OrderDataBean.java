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

public class OrderDataBean  {
    private java.lang.Integer orderID;
    private java.lang.String orderType;
    private java.lang.String orderStatus;
    private java.util.Calendar openDate;
    private java.util.Calendar completionDate;
    private double quantity;
    private java.math.BigDecimal price;
    private java.math.BigDecimal orderFee;
    private java.lang.String symbol;

    public OrderDataBean() {
    }

	public String toString() {
		return "Order " + getOrderID()
				+ "\n\t      orderType: " + getOrderType()
				+ "\n\t    orderStatus: " +	getOrderStatus()
				+ "\n\t       openDate: " +	getOpenDate()
				+ "\n\t completionDate: " +	getCompletionDate()
				+ "\n\t       quantity: " +	getQuantity()
				+ "\n\t          price: " +	getPrice()
				+ "\n\t       orderFee: " +	getOrderFee()
				+ "\n\t         symbol: " +	getSymbol()				
				;
	}

    public java.lang.Integer getOrderID() {
        return orderID;
    }

    public void setOrderID(java.lang.Integer orderID) {
        this.orderID = orderID;
    }

    public java.lang.String getOrderType() {
        return orderType;
    }

    public void setOrderType(java.lang.String orderType) {
        this.orderType = orderType;
    }

    public java.lang.String getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(java.lang.String orderStatus) {
        this.orderStatus = orderStatus;
    }

    public java.util.Calendar getOpenDate() {
        return openDate;
    }

    public void setOpenDate(java.util.Calendar openDate) {
        this.openDate = openDate;
    }

    public java.util.Calendar getCompletionDate() {
        return completionDate;
    }

    public void setCompletionDate(java.util.Calendar completionDate) {
        this.completionDate = completionDate;
    }

    public double getQuantity() {
        return quantity;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }

    public java.math.BigDecimal getPrice() {
        return price;
    }

    public void setPrice(java.math.BigDecimal price) {
        this.price = price;
    }

    public java.math.BigDecimal getOrderFee() {
        return orderFee;
    }

    public void setOrderFee(java.math.BigDecimal orderFee) {
        this.orderFee = orderFee;
    }

    public java.lang.String getSymbol() {
        return symbol;
    }

    public void setSymbol(java.lang.String symbol) {
        this.symbol = symbol;
    }

}
