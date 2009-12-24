/*
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Australian National University - adaptation to DaCapo benchmark suite
 */
package org.eclipse.jdt.core.tests.performance;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.TypeNameRequestor;
import org.eclipse.jdt.core.tests.model.AbstractJavaModelTests;

import org.eclipse.jdt.internal.core.search.processing.IJob;

/**
 * The original unmodified source this class can be found within
 *    eclipse/plugins/org.eclipse.sdk.tests.source_3.5.0.v20090227/src/org.eclipse.jdt.core.tests.performance_3.3.100.v_972_R35x
 *  which can be found within
 *    eclipse-testing/eclipse-junit-tests-M20090917-0800.zip
 *  which can be found within
 *    eclipse-Automated-Tests-3.5.1.zip
 *  which can be downloaded from the eclipse web site
 *  
 * @date $Date: 2009-12-24 11:19:36 +1100 (Thu, 24 Dec 2009) $
 * @id $Id: FullSourceWorkspaceSearchTests.java 738 2009-12-24 00:19:36Z steveb-oss $
 */
public class FullSourceWorkspaceSearchTests extends FullSourceWorkspaceTests implements IJavaSearchConstants {

  public static void runDaCapoTests() {
    try {
      FullSourceWorkspaceSearchTests s = new FullSourceWorkspaceSearchTests();
      if (DACAPO_PRINT) System.out.print("Index workspace ");
      s.testIndexing();
      if (DACAPO_PRINT) System.out.println();
    } catch (Exception e) {
      System.err.println("Caught exception performing search tests: ");
      e.printStackTrace();
    }
  }
  
  private static final int WAIT_UNTIL_READY_TO_SEARCH = 0;

  /**
   * Performance tests for search: Indexing entire workspace
   *
   * First wait that already started indexing jobs ends before performing test and measure.
   * Consider this initial indexing jobs as warm-up for this test.
   */
  public void testIndexing() throws CoreException {

    // Wait for indexing end (we use initial indexing as warm-up)
    AbstractJavaModelTests.waitUntilIndexesReady();

    // Remove project previous indexing
    INDEX_MANAGER.removeIndexFamily(new Path(""));
    INDEX_MANAGER.reset();

    // Restart brand new indexing
    INDEX_MANAGER.request(new Measuring(true));
    for (int j=0, length=ALL_PROJECTS.length; j<length; j++) {
      if (DACAPO_PRINT) System.out.print(".");
      INDEX_MANAGER.indexAll(ALL_PROJECTS[j].getProject());
    }
    waitUntilIndexesReady();
  }
  
  protected void waitUntilIndexesReady() {
    if (DACAPO_PRINT) System.out.print(".");
    AbstractJavaModelTests.waitUntilIndexesReady();
    if (DACAPO_PRINT) System.out.print(".");
  }

  /**
   * Simple type name requestor: only count classes and interfaces.
   */
  class SearchTypeNameRequestor extends TypeNameRequestor {
    int count = 0;
    public void acceptType(int modifiers, char[] packageName, char[] simpleTypeName, char[][] enclosingTypeNames, String path) {
      this.count++;
      if (DACAPO_PRINT && (this.count % 500 == 0)) System.out.print(".");
    }
  }
  
  /**
   * Job to measure times in same thread than index manager.
   */
  class Measuring implements IJob {
    boolean start;
    Measuring(boolean start) {
      this.start = start;
    }
    public boolean belongsTo(String jobFamily) {
      return true;
    }
    public void cancel() {}
    public void ensureReadyToRun() {}
    /**
     * Execute the current job, answer whether it was successful.
     */
    public boolean execute(IProgressMonitor progress) {
      return true;
    }
    public String getJobFamily() {
      return "FullSourceWorkspaceSearchTests.Measuring";
    }
  }
}
