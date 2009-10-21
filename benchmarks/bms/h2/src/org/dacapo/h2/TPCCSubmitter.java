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
 * A TPC-C like Submitter that will execute a fixed number of transactions
 * only counting those transactions that succeed.  Failed transactions
 * are ignored and another transaction is tried.
 */
public class TPCCSubmitter extends Submitter {
	// percentage of transactions that we will tolerate failing 
	// before giving up as a failed run.
	private final static int MAXIMUM_FAILURE_PERCENTAGE = 10;

	public TPCCSubmitter(Display display, Operations ops, OERandom rand,
			short maxW) {
		super(display, ops, rand, maxW);
	}

	@Override
	public long runTransactions(final Object displayData, final int count)
		throws Exception
	{
		int  failures = 0;
		int  failure_limit = (count * MAXIMUM_FAILURE_PERCENTAGE)/100;
        for (int i = 0; i < count && failures <= failure_limit;)
        {
        	// failed transactions will be ignored an another transaction tried.
        	try
        	{
        		runTransaction(displayData);
                i++;
                if ((i%50)==0) System.out.print(".");
        	} catch (Exception e) {
        		failure_limit++;
        	}
        }
        
        // timing is done else where
        return 0;
	}
}
