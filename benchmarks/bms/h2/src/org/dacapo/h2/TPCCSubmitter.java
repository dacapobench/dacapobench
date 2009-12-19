/* 
 *
 * Dacapo benchmark harness for TPC-C like workload running on H2.
 * 
 * Apache authored the original TPC-C like workload.
 * 
 * @author Apache
 */
package org.dacapo.h2;

import org.apache.derbyTesting.system.oe.client.Display;
import org.apache.derbyTesting.system.oe.client.Operations;
import org.apache.derbyTesting.system.oe.client.Submitter;
import org.apache.derbyTesting.system.oe.util.OERandom;

/**
 * A TPC-C like Submitter that will execute a fixed number of transactions only
 * counting those transactions that succeed. Failed transactions are ignored and
 * another transaction is tried.
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

  public TPCCSubmitter(TPCCReporter reporter, Operations ops, OERandom rand,
      short maxW) {
    super(null, ops, rand, maxW);
    this.rand = rand;
    this.reporter = reporter;
  }

  @Override
  public long runTransactions(final Object displayData, final int count)
      throws Exception {
    for (int i = 0; i < count; i++) {
      rand.setSeed(getNextSeed());

      int txType = getTransactionType();
      boolean success = false;
      while (!success) {
        try {
          success = runTransaction(txType, displayData);
        } catch (Exception e) {}
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
