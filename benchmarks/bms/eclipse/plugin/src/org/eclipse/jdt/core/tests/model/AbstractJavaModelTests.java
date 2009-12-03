/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Australian National University - adaptation to DaCapo test harness
 *******************************************************************************/
package org.eclipse.jdt.core.tests.model;

/*
 *  The original source for this class can be found within:
 *    eclipse/plugins/org.eclipse.sdk.tests.source_3.5.0.v20090227/src/org.eclipse.jdt.core.tests.model_3.3.100.v_972_R35x/jdtcoretestsmodelsrc.zip
 *  which is packaged within
 *    eclipse-junit-tests-M20090917-0800.zip
 *  which is packaged within
 *    eclipse-Automated-Tests-3.5.1.zip
 *  which is downloadable from the eclipse web site
 */
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.TypeNameRequestor;

public class AbstractJavaModelTests {
  public static void waitForAutoBuild() {
    boolean wasInterrupted = false;
    do {
      try {
        Job.getJobManager().join(ResourcesPlugin.FAMILY_AUTO_BUILD, null);
        wasInterrupted = false;
      } catch (OperationCanceledException e) {
        e.printStackTrace();
      } catch (InterruptedException e) {
        wasInterrupted = true;
      }
    } while (wasInterrupted);
  }

  public static void waitUntilIndexesReady() {
    // dummy query for waiting until the indexes are ready
    SearchEngine engine = new SearchEngine();
    IJavaSearchScope scope = SearchEngine.createWorkspaceScope();
    try {
      engine.searchAllTypeNames(null, SearchPattern.R_EXACT_MATCH, "!@$#!@"
          .toCharArray(), SearchPattern.R_PATTERN_MATCH
          | SearchPattern.R_CASE_SENSITIVE, IJavaSearchConstants.CLASS, scope,
          new TypeNameRequestor() {
            public void acceptType(int modifiers, char[] packageName,
                char[] simpleTypeName, char[][] enclosingTypeNames, String path) {
            }
          }, IJavaSearchConstants.WAIT_UNTIL_READY_TO_SEARCH, null);
    } catch (CoreException e) {
    }
  }
}
