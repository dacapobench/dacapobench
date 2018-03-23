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

public class AccountProfileDataBean  {
    private java.lang.String userID;
    private java.lang.String password;
    private java.lang.String fullName;
    private java.lang.String address;
    private java.lang.String email;
    private java.lang.String creditCard;

    public AccountProfileDataBean() {
    }

	public String toString() {
		return "\n\tAccount Profile Data for userID:" + getUserID()
			+ "\n\t\t   password:" + getPassword()
			+ "\n\t\t   fullName:" + getFullName()
			+ "\n\t\t    address:" + getAddress()
			+ "\n\t\t      email:" + getEmail()
			+ "\n\t\t creditCard:" + getCreditCard()
			;
	}

    public java.lang.String getUserID() {
        return userID;
    }

    public void setUserID(java.lang.String userID) {
        this.userID = userID;
    }

    public java.lang.String getPassword() {
        return password;
    }

    public void setPassword(java.lang.String password) {
        this.password = password;
    }

    public java.lang.String getFullName() {
        return fullName;
    }

    public void setFullName(java.lang.String fullName) {
        this.fullName = fullName;
    }

    public java.lang.String getAddress() {
        return address;
    }

    public void setAddress(java.lang.String address) {
        this.address = address;
    }

    public java.lang.String getEmail() {
        return email;
    }

    public void setEmail(java.lang.String email) {
        this.email = email;
    }

    public java.lang.String getCreditCard() {
        return creditCard;
    }

    public void setCreditCard(java.lang.String creditCard) {
        this.creditCard = creditCard;
    }

}
