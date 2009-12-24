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

import java.text.NumberFormat;

import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.search.*;
import org.eclipse.jdt.core.tests.model.AbstractJavaModelTests;

/**
 * The original source for this class can be found within:
 * eclipse/plugins/org.eclipse
 * .sdk.tests.source_3.5.0.v20090227/src/org.eclipse.jdt
 * .core.tests.model_3.3.100.v_972_R35x/jdtcoretestsmodelsrc.zip which is
 * packaged within eclipse-junit-tests-M20090917-0800.zip which is packaged
 * within eclipse-Automated-Tests-3.5.1.zip which is downloadable from the
 * eclipse web site
 * 
 * @date $Date: 2009-12-24 11:19:36 +1100 (Thu, 24 Dec 2009) $
 * @id $Id: FullSourceWorkspaceCompleteSearchTests.java 738 2009-12-24 00:19:36Z steveb-oss $
 */
public class FullSourceWorkspaceCompleteSearchTests extends FullSourceWorkspaceSearchTests {

  public static void runDaCapoTests() {
    try {
      FullSourceWorkspaceCompleteSearchTests s = new FullSourceWorkspaceCompleteSearchTests();
      if (DACAPO_PRINT)
        System.out.print("Search ");
      s.testSearchStringConstructorReferences();
      if (DACAPO_PRINT) {
        System.out.println();
        System.out.print("       ");
      }
      s.testSearchStringMethodReferences();
      if (DACAPO_PRINT)
        System.out.println();
    } catch (Exception e) {
      System.err.println("Caught exception performing search tests: ");
      e.printStackTrace();
    }
  }

  protected void search(IJavaElement element, int limitTo, JavaSearchResultCollector resultCollector) throws CoreException {
    SearchPattern pattern = SearchPattern.createPattern(element, limitTo, SearchPattern.R_EXACT_MATCH | SearchPattern.R_CASE_SENSITIVE);
    new SearchEngine().search(pattern, new SearchParticipant[] { SearchEngine.getDefaultSearchParticipant() }, SearchEngine.createWorkspaceScope(),
        resultCollector, null);
  }

  /**
   * Simple search result collector: only count matches.
   */
  class JavaSearchResultCollector extends SearchRequestor {
    int count = 0;

    public void acceptSearchMatch(SearchMatch match) throws CoreException {
      this.count++;
    }
  }

  protected void search(String patternString, int searchFor, int limitTo, JavaSearchResultCollector resultCollector) throws CoreException {
    int matchMode = patternString.indexOf('*') != -1 || patternString.indexOf('?') != -1 ? SearchPattern.R_PATTERN_MATCH : SearchPattern.R_EXACT_MATCH;
    SearchPattern pattern = SearchPattern.createPattern(patternString, searchFor, limitTo, matchMode | SearchPattern.R_CASE_SENSITIVE);

    IJavaSearchScope scope = org.eclipse.jdt.core.search.SearchEngine.createJavaSearchScope(ALL_PROJECTS, org.eclipse.jdt.core.search.IJavaSearchScope.SOURCES);
    new SearchEngine().search(pattern, new SearchParticipant[] { SearchEngine.getDefaultSearchParticipant() }, scope, resultCollector, null);
  }

  /**
   * Clean last category table cache
   * @param type Tells whether previous search was a type search or not
   * @param resultCollector result collector to count the matches found
   */
  protected void cleanCategoryTableCache(boolean type, JavaSearchResultCollector resultCollector) throws CoreException {
    long time = System.currentTimeMillis();
    if (type) {
      search("foo", FIELD, DECLARATIONS, resultCollector);
    } else {
      search("Foo", TYPE, DECLARATIONS, resultCollector);
    }
    if (DEBUG)
      System.out.println("Time to clean category table cache: " + (System.currentTimeMillis() - time));
  }

  public void testSearchStringConstructorReferences() throws CoreException {

    // Wait for indexing end
    waitUntilIndexesReady();

    // Warm up
    String name = "()";
    JavaSearchResultCollector resultCollector = new JavaSearchResultCollector();
    search(name, CONSTRUCTOR, REFERENCES, resultCollector);
    NumberFormat intFormat = NumberFormat.getIntegerInstance();
    if (DACAPO_PRINT)
      System.out.print(" " + intFormat.format(resultCollector.count) + " references for default constructor in workspace");
  }

  public void testSearchStringMethodReferences() throws CoreException {

    // Wait for indexing end
    waitUntilIndexesReady();

    // Warm up
    String name = "equals";
    JavaSearchResultCollector resultCollector = new JavaSearchResultCollector();
    search(name, METHOD, REFERENCES, resultCollector);
    NumberFormat intFormat = NumberFormat.getIntegerInstance();
    if (DACAPO_PRINT)
      System.out.print(" " + intFormat.format(resultCollector.count) + " references for method '" + name + "' in workspace");
  }
}
