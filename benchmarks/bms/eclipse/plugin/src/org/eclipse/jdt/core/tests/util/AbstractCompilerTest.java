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
package org.eclipse.jdt.core.tests.util;

import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;

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
 * @id $Id: AbstractCompilerTest.java 738 2009-12-24 00:19:36Z steveb-oss $
 */
public class AbstractCompilerTest {
  public static final int F_1_3 = 0x01;
  public static final int F_1_4 = 0x02;
  public static final int F_1_5 = 0x04;
  public static final int F_1_6 = 0x08;
  public static final int F_1_7 = 0x10;

  public static final boolean RUN_JAVAC = CompilerOptions.ENABLED.equals(System.getProperty("run.javac"));
  private static final int UNINITIALIZED = -1;
  private static final int NONE = 0;
  private static int possibleComplianceLevels = UNINITIALIZED;

  /*
   * Returns the possible compliance levels this VM instance can run.
   */
  public static int getPossibleComplianceLevels() {
    if (possibleComplianceLevels == UNINITIALIZED) {
      String compliance = System.getProperty("compliance");
      if (compliance != null) {
        if (CompilerOptions.VERSION_1_3.equals(compliance)) {
          possibleComplianceLevels = RUN_JAVAC ? NONE : F_1_3;
        } else if (CompilerOptions.VERSION_1_4.equals(compliance)) {
          possibleComplianceLevels = RUN_JAVAC ? NONE : F_1_4;
        } else if (CompilerOptions.VERSION_1_5.equals(compliance)) {
          possibleComplianceLevels = F_1_5;
        } else if (CompilerOptions.VERSION_1_6.equals(compliance)) {
          possibleComplianceLevels = F_1_6;
        } else if (CompilerOptions.VERSION_1_7.equals(compliance)) {
          possibleComplianceLevels = F_1_7;
        } else {
          System.out.println("Invalid compliance specified (" + compliance + ")");
          System.out.print("Use one of ");
          System.out.print(CompilerOptions.VERSION_1_3 + ", ");
          System.out.print(CompilerOptions.VERSION_1_4 + ", ");
          System.out.print(CompilerOptions.VERSION_1_5 + ", ");
          System.out.print(CompilerOptions.VERSION_1_6 + ", ");
          System.out.println(CompilerOptions.VERSION_1_7);
          System.out.println("Defaulting to all possible compliances");
        }
      }
      if (possibleComplianceLevels == UNINITIALIZED) {
        String specVersion = System.getProperty("java.specification.version");
        if (!RUN_JAVAC) {
          possibleComplianceLevels = F_1_3;
          boolean canRun1_4 = !"1.0".equals(specVersion) && !CompilerOptions.VERSION_1_1.equals(specVersion)
              && !CompilerOptions.VERSION_1_2.equals(specVersion) && !CompilerOptions.VERSION_1_3.equals(specVersion);
          if (canRun1_4) {
            possibleComplianceLevels |= F_1_4;
          }
          boolean canRun1_5 = canRun1_4 && !CompilerOptions.VERSION_1_4.equals(specVersion);
          if (canRun1_5) {
            possibleComplianceLevels |= F_1_5;
          }
          boolean canRun1_6 = canRun1_5 && !CompilerOptions.VERSION_1_5.equals(specVersion);
          if (canRun1_6) {
            possibleComplianceLevels |= F_1_6;
          }
          boolean canRun1_7 = canRun1_6 && !CompilerOptions.VERSION_1_6.equals(specVersion);
          if (canRun1_7) {
            possibleComplianceLevels |= F_1_7;
          }
        } else if ("1.0".equals(specVersion) || CompilerOptions.VERSION_1_1.equals(specVersion) || CompilerOptions.VERSION_1_2.equals(specVersion)
            || CompilerOptions.VERSION_1_3.equals(specVersion) || CompilerOptions.VERSION_1_4.equals(specVersion)) {
          possibleComplianceLevels = NONE;
        } else {
          possibleComplianceLevels = F_1_5;
          if (!CompilerOptions.VERSION_1_5.equals(specVersion)) {
            possibleComplianceLevels |= F_1_6;
            if (!CompilerOptions.VERSION_1_6.equals(specVersion)) {
              possibleComplianceLevels |= F_1_7;
            }
          }
        }
      }
    }
    if (possibleComplianceLevels == NONE) {
      System.out.println("Skipping all compliances (found none compatible with run.javac=enabled).");
    }
    return possibleComplianceLevels;
  }
}
