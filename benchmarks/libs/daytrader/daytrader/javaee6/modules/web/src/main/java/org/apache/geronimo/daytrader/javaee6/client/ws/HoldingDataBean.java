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

public class HoldingDataBean  {
    private java.lang.Integer holdingID;
    private double quantity;
    private java.math.BigDecimal purchasePrice;
    private java.util.Calendar purchaseDate;
    private java.lang.String quoteID;

    public HoldingDataBean() {
    }

	public String toString() {
		return "\n\tHolding Data for holding: " + getHoldingID() 
			+ "\n\t\t      quantity:" + getQuantity()
			+ "\n\t\t purchasePrice:" + getPurchasePrice()
			+ "\n\t\t  purchaseDate:" + getPurchaseDate()
			+ "\n\t\t       quoteID:" + getQuoteID()
			;
	}

    public java.lang.Integer getHoldingID() {
        return holdingID;
    }

    public void setHoldingID(java.lang.Integer holdingID) {
        this.holdingID = holdingID;
    }

    public double getQuantity() {
        return quantity;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }

    public java.math.BigDecimal getPurchasePrice() {
        return purchasePrice;
    }

    public void setPurchasePrice(java.math.BigDecimal purchasePrice) {
        this.purchasePrice = purchasePrice;
    }

    public java.util.Calendar getPurchaseDate() {
        return purchaseDate;
    }

    public void setPurchaseDate(java.util.Calendar purchaseDate) {
        this.purchaseDate = purchaseDate;
    }

    public java.lang.String getQuoteID() {
        return quoteID;
    }

    public void setQuoteID(java.lang.String quoteID) {
        this.quoteID = quoteID;
    }

}
