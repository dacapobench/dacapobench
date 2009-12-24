/*
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.*;

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
 * @id $Id: FullSourceWorkspaceASTTests.java 738 2009-12-24 00:19:36Z steveb-oss $
 */
public class FullSourceWorkspaceASTTests extends FullSourceWorkspaceTests {

  public static void runDaCapoTests() {
    try {
      FullSourceWorkspaceASTTests a = new FullSourceWorkspaceASTTests();
      if (DACAPO_PRINT)
        System.out.print("AST tests ");
      a.testDomAstCreationProjectJLS3();
      if (DACAPO_PRINT)
        System.out.println();
    } catch (Exception e) {
      System.err.println("Caught exception performing AST tests: ");
      e.printStackTrace();
    }
  }

  /*
   * Create AST nodes for all compilation unit of a given project
   */
  private void runAstCreation(IJavaProject javaProject) throws Exception {
    if (DEBUG)
      System.out.println("Creating AST for project" + javaProject.getElementName());
    ASTParser parser = ASTParser.newParser(AST.JLS3);
    parser.setResolveBindings(true);
    parser.setProject(javaProject);

    Map options = javaProject.getOptions(true);
    // turn all errors and warnings into ignore. The customizable set of
    // compiler
    // options only contains additional Eclipse options. The standard JDK
    // compiler
    // options can't be changed anyway.
    for (Iterator iter = options.keySet().iterator(); iter.hasNext();) {
      String key = (String) iter.next();
      String value = (String) options.get(key);
      if ("error".equals(value) || "warning".equals(value)) { //$NON-NLS-1$//$NON-NLS-2$
        // System.out.println("Ignoring - " + key);
        options.put(key, "ignore"); //$NON-NLS-1$
      } else if ("enabled".equals(value)) {
        // System.out.println("         - disabling " + key);
        options.put(key, "disabled");
      }
    }
    options.put(JavaCore.COMPILER_TASK_TAGS, "");
    parser.setCompilerOptions(options);

    List units = getProjectCompilationUnits(javaProject);
    ICompilationUnit[] compilationUnits = new ICompilationUnit[units.size()];
    units.toArray(compilationUnits);

    if (PRINT) {
      System.out.println("            - options: " + options);
      System.out.println("            - " + compilationUnits.length + " units will be parsed in " + javaProject.getElementName() + " project");
    }

    // warm up
    parser.createASTs(compilationUnits, new String[0], new ASTRequestor() {
      public void acceptAST(ICompilationUnit source, CompilationUnit ast) {
        IProblem[] problems = ast.getProblems();
        int length = problems.length;
        if (length > 0) {
          StringBuffer buffer = new StringBuffer();
          for (int i = 0; i < length; i++) {
            buffer.append(problems[i].getMessage());
            buffer.append('\n');
          }
        }
      }
    }, null);

    parser.createASTs(compilationUnits, new String[0], new ASTRequestor() {/*
                                                                            * do
                                                                            * nothing
                                                                            */
    }, null);
  }

  /**
   * Create AST nodes tree for all compilation units in JUnit project.
   * 
   * @throws JavaModelException
   */
  public void testDomAstCreationProjectJLS3() throws Exception {
    runAstCreation(getProject("org.eclipse.core.runtime"));
  }
}
