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
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jdt.internal.formatter.DefaultCodeFormatter;

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
 * @id $Id: FullSourceWorkspaceFormatterTests.java 738 2009-12-24 00:19:36Z steveb-oss $
 */
public class FullSourceWorkspaceFormatterTests extends FullSourceWorkspaceTests {

  public static void runDaCapoTests() {
    try {
      FullSourceWorkspaceFormatterTests f = new FullSourceWorkspaceFormatterTests();
      if (DACAPO_PRINT)
        System.out.print("Format tests ");
      f.testFormatDefault();
      if (DACAPO_PRINT)
        System.out.println();
    } catch (Exception e) {
      System.err.println("Caught exception performing search tests: ");
      e.printStackTrace();
    }
  }

  /* selection of the largest source files in the default workspace */
  static final String[] FORMAT_FILES = {
    "org.eclipse.jdt.internal.core.JavaProject",
    "org.eclipse.jdt.internal.compiler.ClassFile",
    "org.eclipse.jdt.internal.core.util.PublicScanner",
    "org.eclipse.jdt.internal.compiler.parser.Scanner",
    "org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants",
    "org.eclipse.jdt.internal.codeassist.CompletionEngine",
    "org.eclipse.jdt.internal.compiler.codegen.ConstantPool",
    "org.eclipse.jdt.internal.formatter.DefaultCodeFormatterOptions",
    "org.eclipse.jdt.internal.compiler.problem.ProblemReporter",
    "org.eclipse.jdt.core.dom.ASTConverter",
    "org.eclipse.jdt.internal.compiler.codegen.CodeStream",
    "org.eclipse.jdt.internal.formatter.CodeFormatterVisitor",
    "org.eclipse.jdt.core.JavaCore",
    "org.eclipse.jdt.internal.compiler.parser.Parser"
  };

  /**
   * Format files using code formatter default options.
   */
  public void testFormatDefault() throws CoreException {
    for (int i = 0; i < FORMAT_FILES.length; i++) {
      if (DACAPO_PRINT)
        System.out.print(".");
      IJavaElement element = JDT_CORE_PROJECT.findType(FORMAT_FILES[i]);
      String source = ((ICompilationUnit) element.getParent()).getSource();
      for (int j = 0; j < 2; j++)
        new DefaultCodeFormatter().format(CodeFormatter.K_COMPILATION_UNIT, source, 0, source.length(), 0, null);
    }
  }
}
