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

public class MarketSummaryDataBeanWS  {
    private java.math.BigDecimal TSIA;
    private java.math.BigDecimal openTSIA;
    private double volume;
    private org.apache.geronimo.daytrader.javaee6.client.ws.QuoteDataBean[] topGainers;
    private org.apache.geronimo.daytrader.javaee6.client.ws.QuoteDataBean[] topLosers;
    private java.util.Calendar summaryDate;

    public MarketSummaryDataBeanWS() {
    }

	public String toString() {
		String ret = "\n\tMarket Summary at: " + getSummaryDate()
			+ "\n\t\t        TSIA:" + getTSIA()
			+ "\n\t\t    openTSIA:" + getOpenTSIA()
			+ "\n\t\t      volume:" + getVolume()
			;

		if ( (getTopGainers()==null) || (getTopLosers()==null) )
			return ret;
		ret += "\n\t\t   Current Top Gainers:";
		for (int ii = 0; ii < topGainers.length; ii++) {
			QuoteDataBean quoteData = topGainers[ii];
			ret += ( "\n\t\t\t"  + quoteData.toString() );
		}
		ret += "\n\t\t   Current Top Losers:";
		for (int ii = 0; ii < topLosers.length; ii++) {
			QuoteDataBean quoteData = topLosers[ii];
			ret += ( "\n\t\t\t"  + quoteData.toString() );
		}
		return ret;		
	}

    public java.math.BigDecimal getTSIA() {
        return TSIA;
    }

    public void setTSIA(java.math.BigDecimal TSIA) {
        this.TSIA = TSIA;
    }

    public java.math.BigDecimal getOpenTSIA() {
        return openTSIA;
    }

    public void setOpenTSIA(java.math.BigDecimal openTSIA) {
        this.openTSIA = openTSIA;
    }

    public double getVolume() {
        return volume;
    }

    public void setVolume(double volume) {
        this.volume = volume;
    }

    public org.apache.geronimo.daytrader.javaee6.client.ws.QuoteDataBean[] getTopGainers() {
        return topGainers;
    }

    public void setTopGainers(org.apache.geronimo.daytrader.javaee6.client.ws.QuoteDataBean[] topGainers) {
        this.topGainers = topGainers;
    }

    public org.apache.geronimo.daytrader.javaee6.client.ws.QuoteDataBean[] getTopLosers() {
        return topLosers;
    }

    public void setTopLosers(org.apache.geronimo.daytrader.javaee6.client.ws.QuoteDataBean[] topLosers) {
        this.topLosers = topLosers;
    }

    public java.util.Calendar getSummaryDate() {
        return summaryDate;
    }

    public void setSummaryDate(java.util.Calendar summaryDate) {
        this.summaryDate = summaryDate;
    }

}
