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

public class AccountDataBean  {
    private java.lang.Integer accountID;
    private int loginCount;
    private int logoutCount;
    private java.util.Calendar lastLogin;
    private java.util.Calendar creationDate;
    private java.math.BigDecimal balance;
    private java.math.BigDecimal openBalance;
    private java.lang.String profileID;

    public AccountDataBean() {
    }

	public String toString() {
		return "\n\tAccount Data for account: " + getAccountID()
			+ "\n\t\t   loginCount:" + getLoginCount()
			+ "\n\t\t  logoutCount:" + getLogoutCount()
			+ "\n\t\t    lastLogin:" + getLastLogin()
			+ "\n\t\t creationDate:" + getCreationDate()
			+ "\n\t\t      balance:" + getBalance()
			+ "\n\t\t  openBalance:" + getOpenBalance()
			+ "\n\t\t    profileID:" + getProfileID()			
			;
	}

    public java.lang.Integer getAccountID() {
        return accountID;
    }

    public void setAccountID(java.lang.Integer accountID) {
        this.accountID = accountID;
    }

    public int getLoginCount() {
        return loginCount;
    }

    public void setLoginCount(int loginCount) {
        this.loginCount = loginCount;
    }

    public int getLogoutCount() {
        return logoutCount;
    }

    public void setLogoutCount(int logoutCount) {
        this.logoutCount = logoutCount;
    }

    public java.util.Calendar getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(java.util.Calendar lastLogin) {
        this.lastLogin = lastLogin;
    }

    public java.util.Calendar getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(java.util.Calendar creationDate) {
        this.creationDate = creationDate;
    }

    public java.math.BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(java.math.BigDecimal balance) {
        this.balance = balance;
    }

    public java.math.BigDecimal getOpenBalance() {
        return openBalance;
    }

    public void setOpenBalance(java.math.BigDecimal openBalance) {
        this.openBalance = openBalance;
    }

    public java.lang.String getProfileID() {
        return profileID;
    }

    public void setProfileID(java.lang.String profileID) {
        this.profileID = profileID;
    }

}
