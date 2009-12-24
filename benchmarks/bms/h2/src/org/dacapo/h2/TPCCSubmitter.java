/* 
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
 *     Apache Software Foundation (ASF)
 *     Australian National University - adaptation to DaCapo test harness
 */
package org.dacapo.h2;

import org.apache.derbyTesting.system.oe.client.Display;
import org.apache.derbyTesting.system.oe.client.Operations;
import org.apache.derbyTesting.system.oe.client.Submitter;
import org.apache.derbyTesting.system.oe.util.OERandom;

/**
 * A TPC-C like Submitter that will execute a fixed number of transactions only
 * counting those transactions that succeed. Failed transactions are ignored and
 * another transaction is tried. Based on Apache Derby implementation.
 * 
 * @date $Date: 2009-12-24 11:19:36 +1100 (Thu, 24 Dec 2009) $
 * @id $Id: TPCCSubmitter.java 738 2009-12-24 00:19:36Z steveb-oss $
 */
public class TPCCSubmitter extends Submitter {
  // percentage of transactions that we will tolerate failing
  // before giving up as a failed run.
  private final static int MAXIMUM_FAILURE_PERCENTAGE = 10;

  private static long globalSeed = 0;

  private OERandom rand;
  private TPCCReporter reporter;

  static void setSeed(long seed) {
    globalSeed = seed;
  }

  private synchronized static long getNextSeed() {
    long result = globalSeed;
    globalSeed += TPCC.SEED_STEP;
    return result;
  }

  public TPCCSubmitter(TPCCReporter reporter, Operations ops, OERandom rand, short maxW) {
    super(null, ops, rand, maxW);
    this.rand = rand;
    this.reporter = reporter;
  }

  @Override
  public long runTransactions(final Object displayData, final int count) throws Exception {
    for (int i = 0; i < count; i++) {
      rand.setSeed(getNextSeed());

      int txType = getTransactionType();
      boolean success = false;
      while (!success) {
        try {
          success = runTransaction(txType, displayData);
        } catch (Exception e) {
        }
      }
      transactionCount[txType]++;
      reporter.done();
    }

    // timing is done elsewhere
    return 0;
  }

  private int getTransactionType() {
    int value = rand.randomInt(1, 1000);
    for (int type = 0; type < TX_CUM_PROB.length; type++) {
      if (value <= TX_CUM_PROB[type])
        return type;
    }
    return -1; // unreachable
  }

  private boolean runTransaction(final int txType, final Object displayData) throws Exception {
    switch (txType) {
    case Submitter.STOCK_LEVEL:
      runStockLevel(displayData);
      break;
    case Submitter.ORDER_STATUS_BY_NAME:
      runOrderStatus(displayData, true);
      break;
    case Submitter.ORDER_STATUS_BY_ID:
      runOrderStatus(displayData, false);
      break;
    case Submitter.PAYMENT_BY_NAME:
      runPayment(displayData, true);
      break;
    case Submitter.PAYMENT_BY_ID:
      runPayment(displayData, false);
      break;
    case Submitter.DELIVERY_SCHEDULE:
      runScheduleDelivery(displayData);
      break;
    case Submitter.NEW_ORDER:
      runNewOrder(displayData, false);
      break;
    case Submitter.NEW_ORDER_ROLLBACK:
      runNewOrder(displayData, true);
      break;
    }
    return true;
  }

  private static synchronized void doneTx() {
  }
}
