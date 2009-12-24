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
import org.eclipse.jdt.core.CompletionProposal;
import org.eclipse.jdt.core.CompletionRequestor;
import org.eclipse.jdt.core.ICompilationUnit;
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
 * @id $Id: FullSourceWorkspaceCompletionTests.java 738 2009-12-24 00:19:36Z steveb-oss $
 */
public class FullSourceWorkspaceCompletionTests extends FullSourceWorkspaceTests {

  public static void runDaCapoTests() {
    try {
      FullSourceWorkspaceCompletionTests c = new FullSourceWorkspaceCompletionTests();
      if (DACAPO_PRINT)
        System.out.print("Completion tests ");
      if (DACAPO_PRINT)
        System.out.print(".");
      c.testPerfCompleteEmptyName();
      if (DACAPO_PRINT)
        System.out.print(".");
      c.testPerfCompleteEmptyNameWithoutMethods();
      if (DACAPO_PRINT)
        System.out.print(".");
      c.testPerfCompleteEmptyNameWithoutTypes();
      if (DACAPO_PRINT)
        System.out.print(".");
      c.testPerfCompleteMemberAccess();
      if (DACAPO_PRINT)
        System.out.print(".");
      c.testPerfCompleteMethodDeclaration();
      if (DACAPO_PRINT)
        System.out.print(".");
      c.testPerfCompleteName();
      if (DACAPO_PRINT)
        System.out.print(".");
      c.testPerfCompleteNameWithoutMethods();
      if (DACAPO_PRINT)
        System.out.print(".");
      c.testPerfCompleteNameWithoutTypes();
      if (DACAPO_PRINT)
        System.out.print(".");
      c.testPerfCompleteTypeReference();
      if (DACAPO_PRINT)
        System.out.println();
    } catch (Exception e) {
      System.err.println("Caught exception performing build tests: ");
      e.printStackTrace();
    }
  }

  private static final int WARMUP_COUNT = 0;
  private static final int ITERATION_COUNT = 10;
  static int[] PROPOSAL_COUNTS;
  static int TESTS_COUNT = 0;
  static int TESTS_LENGTH;

  class TestCompletionRequestor extends CompletionRequestor {
    public void accept(CompletionProposal proposal) {
    }
  }

  private void complete(String projectName, String packageName, String unitName, String completeAt, String completeBehind, int warmupCount, int iterationCount)
      throws CoreException {
    complete(projectName, packageName, unitName, completeAt, completeBehind, null, warmupCount, iterationCount);
  }

  private void complete(String projectName, String packageName, String unitName, String completeAt, String completeBehind, int[] ignoredKinds, int warmupCount,
      int iterationCount) throws CoreException {

    AbstractJavaModelTests.waitUntilIndexesReady();

    TestCompletionRequestor requestor = new TestCompletionRequestor();
    if (ignoredKinds != null) {
      for (int i = 0; i < ignoredKinds.length; i++) {
        requestor.setIgnored(ignoredKinds[i], true);
      }
    }

    ICompilationUnit unit = getCompilationUnit(projectName, packageName, unitName);

    String str = unit.getSource();
    int completionIndex = str.indexOf(completeAt) + completeBehind.length();

    if (DEBUG)
      System.out.print("Perform code assist inside " + unitName + "...");

    for (int j = 0; j < iterationCount; j++) {
      unit.codeComplete(completionIndex, requestor);
    }
    if (DEBUG)
      System.out.println("done!");
  }

  public void testPerfCompleteMethodDeclaration() throws CoreException {
    complete("org.eclipse.jdt.core", "org.eclipse.jdt.internal.core", "SourceType.java", "IType {", "IType {", WARMUP_COUNT, ITERATION_COUNT);
  }

  public void testPerfCompleteMemberAccess() throws CoreException {
    complete("org.eclipse.jdt.core", "org.eclipse.jdt.internal.core", "SourceType.java", "this.", "this.", null, WARMUP_COUNT, ITERATION_COUNT);
  }

  public void testPerfCompleteTypeReference() throws CoreException {
    complete("org.eclipse.jdt.core", "org.eclipse.jdt.internal.core", "SourceType.java", "ArrayList list", "A", WARMUP_COUNT, ITERATION_COUNT);
  }

  public void testPerfCompleteEmptyName() throws CoreException {
    complete("org.eclipse.jdt.core", "org.eclipse.jdt.internal.core", "SourceType.java", "params.add", "", WARMUP_COUNT, ITERATION_COUNT);
  }

  public void testPerfCompleteName() throws CoreException {
    complete("org.eclipse.jdt.core", "org.eclipse.jdt.internal.core", "SourceType.java", "params.add", "p", null, WARMUP_COUNT, ITERATION_COUNT);
  }

  public void testPerfCompleteEmptyNameWithoutTypes() throws CoreException {
    complete("org.eclipse.jdt.core", "org.eclipse.jdt.internal.core", "SourceType.java", "params.add", "", new int[] { CompletionProposal.TYPE_REF },
        WARMUP_COUNT, ITERATION_COUNT);
  }

  public void testPerfCompleteNameWithoutTypes() throws CoreException {
    complete("org.eclipse.jdt.core", "org.eclipse.jdt.internal.core", "SourceType.java", "params.add", "p", new int[] { CompletionProposal.TYPE_REF },
        WARMUP_COUNT, ITERATION_COUNT);
  }

  public void testPerfCompleteEmptyNameWithoutMethods() throws CoreException {
    complete("org.eclipse.jdt.core", "org.eclipse.jdt.internal.core", "SourceType.java", "params.add", "", new int[] { CompletionProposal.METHOD_REF },
        WARMUP_COUNT, ITERATION_COUNT);
  }

  public void testPerfCompleteNameWithoutMethods() throws CoreException {
    complete("org.eclipse.jdt.core", "org.eclipse.jdt.internal.core", "SourceType.java", "params.add", "p", new int[] { CompletionProposal.METHOD_REF },
        WARMUP_COUNT, ITERATION_COUNT);
  }
}
