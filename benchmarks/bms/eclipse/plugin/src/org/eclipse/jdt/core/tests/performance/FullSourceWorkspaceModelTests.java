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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.tests.model.AbstractJavaModelTests;

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
 * @id $Id: FullSourceWorkspaceModelTests.java 738 2009-12-24 00:19:36Z steveb-oss $
 */
public class FullSourceWorkspaceModelTests extends FullSourceWorkspaceTests {
  public static void runDaCapoTests() {
    try {
      FullSourceWorkspaceModelTests m = new FullSourceWorkspaceModelTests();
      if (DACAPO_PRINT)
        System.out.print("Model tests ");
      m.testFindType();
      if (DACAPO_PRINT)
        System.out.println();
    } catch (Exception e) {
      System.err.println("Caught exception performing search tests: ");
      e.printStackTrace();
    }
  }

  /*
   * Creates a simple Java project with no source folder and only rt.jar on its
   * classpath.
   */
  private IJavaProject createJavaProject(String name) throws CoreException {
    IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(name);
    if (project.exists())
      project.delete(true, null);
    project.create(null);
    project.open(null);
    IProjectDescription description = project.getDescription();
    description.setNatureIds(new String[] { JavaCore.NATURE_ID });
    project.setDescription(description, null);
    IJavaProject javaProject = JavaCore.create(project);
    javaProject.setRawClasspath(new IClasspathEntry[] { JavaCore.newVariableEntry(new Path("JRE_LIB"), null, null) }, null);
    return javaProject;
  }

  /*
   * Performance test for the first use of findType(...) (see bug 161175
   * JarPackageFragmentRoot slow to initialize)
   */
  public void testFindType() throws CoreException {

    IJavaModel model = JavaCore.create(ResourcesPlugin.getWorkspace().getRoot());
    IJavaProject[] existingProjects = model.getJavaProjects();

    try {
      // close existing projects
      for (int i = 0, length = existingProjects.length; i < length; i++) {
        existingProjects[i].getProject().close(null);
        if (DACAPO_PRINT && i % 4 == 0)
          System.out.print(".");
      }

      // get 20 projects
      int max = 20;
      IJavaProject[] projects = new IJavaProject[max];
      for (int i = 0; i < max; i++) {
        projects[i] = createJavaProject("FindType" + i);
        if (DACAPO_PRINT && i % 4 == 0)
          System.out.print(".");
      }
      AbstractJavaModelTests.waitUntilIndexesReady();
      if (DACAPO_PRINT)
        System.out.print(".");
      AbstractJavaModelTests.waitForAutoBuild();

      try {
        model.close();
        for (int j = 0; j < max; j++) {
          projects[j].findType("java.lang.Object");
          if (DACAPO_PRINT && j % 4 == 0)
            System.out.print(".");
        }
      } finally {
        for (int i = 0; i < max; i++) {
          projects[i].getProject().delete(false, null);
        }
      }
    } finally {
      // reopen existing projects
      for (int i = 0, length = existingProjects.length; i < length; i++) {
        existingProjects[i].getProject().open(null);
      }
    }
  }

}
