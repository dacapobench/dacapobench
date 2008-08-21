/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package dacapo.eclipse;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.internal.core.search.processing.IJob;

/**
 * This class is heavily based on 
 *  org.eclipse.jdt.core.tests.performance.FullSourceWorkspaceTests, and
 *  org.eclipse.jdt.core.tests.performance.FullSourceWorkspaceIndexTests
 */
class EclipseIndexTests extends EclipseTests {
  // Wait for indexing end
  protected static void waitUntilIndexesReady() {
    /**
     * Simple Job which does nothing
     */
    class DoNothing implements IJob {
      /**
       * Answer true if the job belongs to a given family (tag)
       */
      public boolean belongsTo(String jobFamily) {
        return true;
      }
      /**
       * Asks this job to cancel its execution. The cancellation
       * can take an undertermined amount of time.
       */
      public void cancel() {
        // nothing to cancel
      }
      /**
       * Ensures that this job is ready to run.
       */
      public void ensureReadyToRun() {
        // always ready to do nothing
      }
      /**
       * Execute the current job, answer whether it was successful.
       */
      public boolean execute(IProgressMonitor progress) {
        // always succeed to do nothing
        return true;
      }
    }
    
    // Run simple job which does nothing but wait for indexing end
    indexManager.performConcurrentJob(new DoNothing(), IJavaSearchConstants.WAIT_UNTIL_READY_TO_SEARCH, null);
//  assertEquals("Index manager should not have remaining jobs!", 0, indexManager.awaitingJobsCount()); //$NON-NLS-1$
  }	
  
}
