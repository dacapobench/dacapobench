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
    for (int i = 0; i < count;) {
      rand.setSeed(getNextSeed());
      // failed transactions will be ignored an another transaction tried.
      try {
        runTransaction(displayData);
        
        // must have been successful to get here
        i++;
        
        reporter.done();
      } catch (Exception e) {
      }
    }

    // timing is done else where
    return 0;
  }
  
  private static synchronized void doneTx() {
    
  }
}
