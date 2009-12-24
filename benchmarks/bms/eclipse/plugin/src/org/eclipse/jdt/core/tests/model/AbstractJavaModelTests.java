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
package org.eclipse.jdt.core.tests.model;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.TypeNameRequestor;

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
 * @id $Id: AbstractJavaModelTests.java 738 2009-12-24 00:19:36Z steveb-oss $
 */
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
      engine.searchAllTypeNames(null, SearchPattern.R_EXACT_MATCH, "!@$#!@".toCharArray(), SearchPattern.R_PATTERN_MATCH | SearchPattern.R_CASE_SENSITIVE,
          IJavaSearchConstants.CLASS, scope, new TypeNameRequestor() {
            public void acceptType(int modifiers, char[] packageName, char[] simpleTypeName, char[][] enclosingTypeNames, String path) {
            }
          }, IJavaSearchConstants.WAIT_UNTIL_READY_TO_SEARCH, null);
    } catch (CoreException e) {
    }
  }
}
