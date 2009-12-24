/* 
 * Derby - Class org.apache.derbyTesting.system.oe.direct.Standard
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, 
 * software distributed under the License is distributed on an 
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, 
 * either express or implied. See the License for the specific 
 * language governing permissions and limitations under the License.
 * 
 * Contributors:
 *     Apache Software Foundation (ASF) - initial API and implementation
 *     Australian National University - adaptation to DaCapo test harness
 */
package org.dacapo.h2;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.apache.derbyTesting.system.oe.client.Display;
import org.apache.derbyTesting.system.oe.direct.Standard;
import org.apache.derbyTesting.system.oe.routines.Data;
import org.apache.derbyTesting.system.oe.model.Customer;
import org.apache.derbyTesting.system.oe.model.District;
import org.apache.derbyTesting.system.oe.model.Warehouse;

/**
 * Construct a wrapper sub-classs of the
 * org.apache.derbyTesting.system.oe.direct.Standard operations class, which can
 * be found in:
 * db-derby-10.5.3.0-src/java/testing/org/apache/derbyTesting/system
 * /oe/direct/Standard.java in the
 * http://apache.wildit.net.au/db/derby/db-derby-
 * 10.5.3.0/db-derby-10.5.3.0-src.tar.gz
 * 
 * This class is created as the parent class uses a Java call-back for updating
 * C_DATA for a client which Derby understands but H2 does not. Thus, we
 * override both <code>payment</code> methods copying the code from the parent
 * class (Standard) to this class so that we can make a call to the modified
 * version of <code>paymentById</code>.
 * 
 * The method <code>paymentById</code> is taken from the clas Standard and is
 * modified to remove the call-back.
 * 
 * @date $Date: 2009-12-24 11:19:36 +1100 (Thu, 24 Dec 2009) $
 * @id $Id: Operation.java 738 2009-12-24 00:19:36Z steveb-oss $
 */
public final class Operation extends Standard {

  /**
   * @param conn A connection to the derby database.
   * @param retryLimit The maximum number of retries before catastrophic
   * failure.
   * @throws SQLException
   */
  public Operation(Connection conn) throws SQLException {
    super(conn);
  }

  // The following two payment methods are taken verbatim from
  // org.apache.derbyTesting.system.oe.direct.Standard
  // so that we can in effect override the private method paymentById
  /**
   * Payment by customer last name. Section 2.5.2 The CUSTOMER row will be
   * fetched and then updated. This is due to the need to select the specific
   * customer first based upon last name (which will actually fetch and hence
   * lock a number of customers).
   */
  public void payment(Display display, Object displayData, short w, short d, short cw, short cd, String customerLast, String amount) throws Exception {

    PreparedStatement pyCustomerByName = prepareStatement("SELECT C_ID " + "FROM CUSTOMER WHERE C_W_ID = ? AND C_D_ID = ? AND C_LAST = ? " + "ORDER BY C_FIRST");

    // Since so much data is needed for the payment transaction
    // from the customer we don't fill it in as we select the
    // correct customer. Instead we just fetch the identifier
    // and then execute a payment by identifier.
    try {
      pyCustomerByName.setShort(1, cw);
      pyCustomerByName.setShort(2, cd);
      pyCustomerByName.setString(3, customerLast);
      ResultSet rs = pyCustomerByName.executeQuery();

      nameList.clear();
      while (rs.next()) {
        nameList.add(rs.getObject("C_ID"));
      }
      reset(pyCustomerByName);
      if (nameList.isEmpty())
        throw new SQLException("Payment by name - no matching customer " + customerLast);

      // Customer to use is midpoint (with round up) (see 2.5.2.2)
      int mid = nameList.size() / 2;
      if (mid != 0) {
        if (nameList.size() % 2 == 1)
          mid++;
      }

      int c = ((Integer) nameList.get(mid)).intValue();

      paymentById(display, displayData, w, d, cw, cd, c, amount);
    } catch (SQLException e) {
      conn.rollback();
      throw e;
    }

    if (display != null)
      ;
  }

  /**
   * Payment by customer identifier. Section 2.5.2. The CUSTOMER row is update
   * and then fetched.
   * 
   */
  public void payment(Display display, Object displayData, short w, short d, short cw, short cd, int c, final String amount) throws Exception {

    try {
      paymentById(display, displayData, w, d, cw, cd, c, amount);
    } catch (SQLException e) {
      conn.rollback();
      throw e;
    }

    if (display != null)
      ;
  }

  // Modified operations from Standard, replaced due to callback in to Java that
  // is not supported on most SQL.
  /*
   * Objects for re-use within the transactions
   */
  private final Warehouse warehouse = new Warehouse();
  private final District district = new District();
  private final List nameList = new ArrayList();

  private void paymentById(Display display, Object displayData, short w, short d, short cw, short cd, int c, final String s_amount) throws Exception {

    PreparedStatement pyCustomerPayment = prepareStatement("UPDATE CUSTOMER SET C_BALANCE = C_BALANCE - ?, " + "C_YTD_PAYMENT = C_YTD_PAYMENT + ?, "
        + "C_PAYMENT_CNT = C_PAYMENT_CNT + 1 " + "WHERE C_W_ID = ? AND C_D_ID = ? AND C_ID = ?");

    PreparedStatement pyCustomerInfoId = prepareStatement("SELECT C_FIRST, C_MIDDLE, C_LAST, C_BALANCE, " + "C_STREET_1, C_STREET_2, C_CITY, C_STATE, C_ZIP, "
        + "C_PHONE, C_SINCE, C_CREDIT, C_CREDIT_LIM, C_DISCOUNT, C_DATA " + "FROM CUSTOMER WHERE C_W_ID = ? AND C_D_ID = ? AND C_ID = ?");

    // This is removed as the call out to java from the SQL engine did not work
    // from H2, this value can be calculated externally and set so this callback
    // is somewhat redundant.
    PreparedStatement pyCustomerUpdateBadCredit = prepareStatement(
    // "UPDATE CUSTOMER SET C_DATA = " +
    // " BAD_CREDIT_DATA(C_DATA, ?, ?, C_W_ID, C_W_ID, C_ID, ?) " +
    "UPDATE CUSTOMER SET C_DATA = ? " + "WHERE C_W_ID = ? AND C_D_ID = ? AND C_ID = ?");

    PreparedStatement pyCustomerGetData = prepareStatement("SELECT SUBSTR(C_DATA, 1, 200) AS C_DATA_200 "
        + "FROM CUSTOMER WHERE C_W_ID = ? AND C_D_ID = ? AND C_ID = ?");

    PreparedStatement pyDistrictUpdate = prepareStatement("UPDATE DISTRICT SET D_YTD = D_YTD + ? WHERE D_W_ID = ? AND D_ID = ?");
    PreparedStatement pyDistrictInfo = prepareStatement("SELECT D_NAME, D_STREET_1, D_STREET_2, D_CITY, D_STATE, D_ZIP FROM DISTRICT WHERE D_W_ID = ? AND D_ID = ? ");
    PreparedStatement pyWarehouseUpdate = prepareStatement("UPDATE WAREHOUSE SET W_YTD = W_YTD + ? WHERE W_ID = ?");
    PreparedStatement pyWarehouseInfo = prepareStatement("SELECT W_NAME, W_STREET_1, W_STREET_2, W_CITY, W_STATE, W_ZIP " + "FROM WAREHOUSE WHERE W_ID = ?");

    PreparedStatement pyHistory = prepareStatement("INSERT INTO HISTORY(H_C_ID, H_C_D_ID, H_C_W_ID, H_D_ID, H_W_ID, " + "H_AMOUNT, H_DATA, H_DATE) "
        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)");

    Customer customer = new Customer();
    customer.setWarehouse(cw);
    customer.setDistrict(cd);
    customer.setId(c);

    double d_amount = Double.parseDouble(s_amount);

    // Update the customer assuming that they have good credit
    pyCustomerPayment.setDouble(1, d_amount);
    pyCustomerPayment.setDouble(2, d_amount);
    pyCustomerPayment.setShort(3, cw);
    pyCustomerPayment.setShort(4, cd);
    pyCustomerPayment.setInt(5, c);
    pyCustomerPayment.executeUpdate();

    // Get the customer information
    pyCustomerInfoId.setShort(1, cw);
    pyCustomerInfoId.setShort(2, cd);
    pyCustomerInfoId.setInt(3, c);
    ResultSet rs = pyCustomerInfoId.executeQuery();
    rs.next();

    customer.setFirst(rs.getString("C_FIRST"));
    customer.setMiddle(rs.getString("C_MIDDLE"));
    customer.setLast(rs.getString("C_LAST"));
    customer.setBalance(rs.getString("C_BALANCE"));

    customer.setAddress(getAddress(rs, "C_STREET_1"));

    customer.setPhone(rs.getString("C_PHONE"));
    customer.setSince(rs.getTimestamp("C_SINCE"));
    customer.setCredit(rs.getString("C_CREDIT"));
    customer.setCredit_lim(rs.getString("C_CREDIT_LIM"));
    customer.setDiscount(rs.getString("C_DISCOUNT"));
    customer.setData(rs.getString("C_DATA"));
    reset(pyCustomerInfoId);

    // additional work for bad credit customers.
    if ("BC".equals(customer.getCredit())) {
      // The following line is added (and indexes changed) as the field can be
      // calculated externally and the callback mechanism is not usable under
      // H2.
      pyCustomerUpdateBadCredit.setString(1, Data.dataForBadCredit(customer.getData(), w, d, cw, cd, c, new BigDecimal(s_amount)));
      pyCustomerUpdateBadCredit.setShort(2, cw);
      pyCustomerUpdateBadCredit.setShort(3, cd);
      pyCustomerUpdateBadCredit.setInt(4, c);
      pyCustomerUpdateBadCredit.executeUpdate();
      reset(pyCustomerUpdateBadCredit);

      // Need to display the first 200 characters
      // of C_DATA information if the customer has
      // bad credit.
      pyCustomerGetData.setShort(1, cw);
      pyCustomerGetData.setShort(2, cd);
      pyCustomerGetData.setInt(3, c);
      rs = pyCustomerGetData.executeQuery();
      rs.next();
      customer.setData(rs.getString("C_DATA_200"));
      reset(pyCustomerGetData);
    }

    district.clear();
    district.setWarehouse(w);
    district.setId(d);

    // Update DISTRICT
    pyDistrictUpdate.setDouble(1, d_amount);
    pyDistrictUpdate.setShort(2, w);
    pyDistrictUpdate.setShort(3, d);
    pyDistrictUpdate.executeUpdate();
    reset(pyDistrictUpdate);

    // Get the required information from DISTRICT
    pyDistrictInfo.setShort(1, w);
    pyDistrictInfo.setShort(2, d);
    rs = pyDistrictInfo.executeQuery();
    rs.next();
    district.setName(rs.getString("D_NAME"));
    district.setAddress(getAddress(rs, "D_STREET_1"));
    reset(pyDistrictInfo);

    warehouse.clear();
    warehouse.setId(w);

    // Update WAREHOUSE
    pyWarehouseUpdate.setDouble(1, d_amount);
    pyWarehouseUpdate.setShort(2, w);
    pyWarehouseUpdate.executeUpdate();
    reset(pyWarehouseUpdate);

    // Get the required information from WAREHOUSE
    pyWarehouseInfo.setShort(1, w);
    rs = pyWarehouseInfo.executeQuery();
    rs.next();
    warehouse.setName(rs.getString("W_NAME"));
    warehouse.setAddress(getAddress(rs, "W_STREET_1"));
    reset(pyWarehouseInfo);

    Timestamp currentTimeStamp = new Timestamp(System.currentTimeMillis());

    // Insert HISTORY row
    pyHistory.setInt(1, c);
    pyHistory.setShort(2, cd);
    pyHistory.setShort(3, cw);
    pyHistory.setShort(4, d);
    pyHistory.setShort(5, w);
    pyHistory.setDouble(6, d_amount);
    StringBuffer hData = new StringBuffer(24);
    hData.append(warehouse.getName());
    hData.append("    ");
    hData.append(district.getName());
    pyHistory.setString(7, hData.toString());
    pyHistory.setTimestamp(8, currentTimeStamp);
    pyHistory.executeUpdate();
    reset(pyHistory);

    conn.commit();

  }
}
