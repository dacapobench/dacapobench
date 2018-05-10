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

public class QuoteDataBean  {
    private java.lang.String symbol;
    private java.lang.String companyName;
    private java.math.BigDecimal price;
    private java.math.BigDecimal open;
    private java.math.BigDecimal low;
    private java.math.BigDecimal high;
    private double change;
    private double volume;

    public QuoteDataBean() {
    }

	public String toString() {
		return "\n\tQuote Data for: " + getSymbol()
			+ "\n\t\t companyName: " + getCompanyName()
			+ "\n\t\t      volume: " + getVolume()
			+ "\n\t\t       price: " + getPrice()
			+ "\n\t\t        open: " + getOpen()
			+ "\n\t\t         low: " + getLow()
			+ "\n\t\t        high: " + getHigh()
			+ "\n\t\t      change: " + getChange()
			;
	}

    public java.lang.String getSymbol() {
        return symbol;
    }

    public void setSymbol(java.lang.String symbol) {
        this.symbol = symbol;
    }

    public java.lang.String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(java.lang.String companyName) {
        this.companyName = companyName;
    }

    public java.math.BigDecimal getPrice() {
        return price;
    }

    public void setPrice(java.math.BigDecimal price) {
        this.price = price;
    }

    public java.math.BigDecimal getOpen() {
        return open;
    }

    public void setOpen(java.math.BigDecimal open) {
        this.open = open;
    }

    public java.math.BigDecimal getLow() {
        return low;
    }

    public void setLow(java.math.BigDecimal low) {
        this.low = low;
    }

    public java.math.BigDecimal getHigh() {
        return high;
    }

    public void setHigh(java.math.BigDecimal high) {
        this.high = high;
    }

    public double getChange() {
        return change;
    }

    public void setChange(double change) {
        this.change = change;
    }

    public double getVolume() {
        return volume;
    }

    public void setVolume(double volume) {
        this.volume = volume;
    }

}
