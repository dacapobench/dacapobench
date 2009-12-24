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
import org.eclipse.jdt.core.ICompilationUnit;

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
 * @id $Id: FullSourceWorkspaceTypeHierarchyTests.java 738 2009-12-24 00:19:36Z steveb-oss $
 */
public class FullSourceWorkspaceTypeHierarchyTests extends FullSourceWorkspaceTests {

  public static void runDaCapoTests() {
    try {
      if (DACAPO_PRINT)
        System.out.print("Type hierarchy tests ");
      FullSourceWorkspaceTypeHierarchyTests t = new FullSourceWorkspaceTypeHierarchyTests();
      t.testPerfAllTypes();
      if (DACAPO_PRINT)
        System.out.println();
    } catch (Exception e) {
      System.err.println("Caught exception performing build tests: ");
      e.printStackTrace();
    }
  }

  public void testPerfAllTypes() throws CoreException {
    ICompilationUnit unit = getCompilationUnit("org.eclipse.jdt.core", "org.eclipse.jdt.internal.compiler.ast", "ASTNode.java");
    unit.getType("ASTNode").newTypeHierarchy(null).getAllClasses();
  }
}
