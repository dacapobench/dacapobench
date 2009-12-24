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
package org.eclipse.jdt.core.tests.builder;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.tests.util.AbstractCompilerTest;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;

/**
 * @date $Date: 2009-12-24 11:19:36 +1100 (Thu, 24 Dec 2009) $
 * @id $Id: TestingEnvironment.java 738 2009-12-24 00:19:36Z steveb-oss $
 */
public class TestingEnvironment {
  private boolean isOpen = false;
  private IWorkspace workspace = null;
  private Hashtable projects = null;

  public void openEmptyWorkspace() {
    close();
    openWorkspace();
    this.projects = new Hashtable(10);
    setup();
  }

  private void openWorkspace() {
    try {
      closeWorkspace();

      this.workspace = ResourcesPlugin.getWorkspace();

      // turn off auto-build -- the tests determine when builds occur
      IWorkspaceDescription description = this.workspace.getDescription();
      description.setAutoBuilding(false);
      this.workspace.setDescription(description);
    } catch (Exception e) {
      handle(e);
    }
  }

  private void checkAssertion(String message, boolean b) {
    Assert.isTrue(b, message);
  }

  /**
   * Closes the testing environment and frees up any resources. Once the testing
   * environment is closed, it shouldn't be used any more.
   */
  public void close() {
    try {
      if (this.projects != null) {
        Enumeration projectNames = this.projects.keys();
        while (projectNames.hasMoreElements()) {
          String projectName = (String) projectNames.nextElement();
          getJavaProject(projectName).getJavaModel().close();
        }
      }
      closeWorkspace();
    } catch (CoreException e) {
      e.printStackTrace();
    } catch (RuntimeException e) {
      e.printStackTrace();
    }
  }

  /**
   * Close a project from the workspace.
   */
  public void closeProject(IPath projectPath) {
    checkAssertion("a workspace must be open", this.isOpen); //$NON-NLS-1$
    try {
      getJavaProject(projectPath).getProject().close(null);
    } catch (CoreException e) {
      e.printStackTrace();
    }
  }

  private void closeWorkspace() {
    this.isOpen = false;
  }

  private void setup() {
    this.isOpen = true;
  }

  void handle(Exception e) {
    if (e instanceof CoreException) {
      handleCoreException((CoreException) e);
    } else {
      e.printStackTrace();
      Assert.isTrue(false);
    }
  }

  /**
   * Handles a core exception thrown during a testing environment operation
   */
  private void handleCoreException(CoreException e) {
    e.printStackTrace();
    IStatus status = e.getStatus();
    String message = e.getMessage();
    if (status.isMultiStatus()) {
      MultiStatus multiStatus = (MultiStatus) status;
      IStatus[] children = multiStatus.getChildren();
      StringBuffer buffer = new StringBuffer();
      for (int i = 0, max = children.length; i < max; i++) {
        IStatus child = children[i];
        if (child != null) {
          buffer.append(child.getMessage());
          buffer.append(System.getProperty("line.separator"));//$NON-NLS-1$
          Throwable childException = child.getException();
          if (childException != null) {
            childException.printStackTrace();
          }
        }
      }
      message = buffer.toString();
    }
    Assert.isTrue(false, "Core exception in testing environment: " + message); //$NON-NLS-1$
  }

  public IJavaProject getJavaProject(IPath projectPath) {
    IJavaProject javaProject = JavaCore.create(getProject(projectPath));
    Assert.isNotNull(javaProject);
    return javaProject;
  }

  /**
   * Returns the Java Model element for the project.
   */
  public IJavaProject getJavaProject(String projectName) {
    IJavaProject javaProject = JavaCore.create(getProject(projectName));
    Assert.isNotNull(javaProject);
    return javaProject;
  }

  /**
   * Returns the core project.
   */
  public IProject getProject(String projectName) {
    return (IProject) this.projects.get(projectName);
  }

  private IProject createProject(String projectName) {
    final IProject project = this.workspace.getRoot().getProject(projectName);
    try {
      IWorkspaceRunnable create = new IWorkspaceRunnable() {
        public void run(IProgressMonitor monitor) throws CoreException {
          project.create(null, null);
          project.open(null);
        }
      };
      this.workspace.run(create, null);
      this.projects.put(projectName, project);
      addBuilderSpecs(projectName);
    } catch (CoreException e) {
      handle(e);
    }
    return project;
  }

  private void addBuilderSpecs(String projectName) {
    try {
      IProject project = getProject(projectName);
      IProjectDescription description = project.getDescription();
      description.setNatureIds(new String[] { JavaCore.NATURE_ID });
      project.setDescription(description, null);
    } catch (CoreException e) {
      handleCoreException(e);
    }
  }

  /**
   * Returns the core project.
   */
  public IProject getProject(IPath projectPath) {
    return (IProject) this.projects.get(projectPath.lastSegment());
  }

  public void addProject(IProject project) {
    this.projects.put(project.getName(), project);
  }

  public IPath addProject(String projectName) {
    return addProject(projectName, "1.4");
  }

  public IPath addProject(String projectName, String compliance) {
    checkAssertion("a workspace must be open", this.isOpen); //$NON-NLS-1$
    IProject project = createProject(projectName);
    int requiredComplianceFlag = 0;
    String compilerVersion = null;
    if ("1.5".equals(compliance)) {
      requiredComplianceFlag = AbstractCompilerTest.F_1_5;
      compilerVersion = CompilerOptions.VERSION_1_5;
    } else if ("1.6".equals(compliance)) {
      requiredComplianceFlag = AbstractCompilerTest.F_1_6;
      compilerVersion = CompilerOptions.VERSION_1_6;
    } else if ("1.7".equals(compliance)) {
      requiredComplianceFlag = AbstractCompilerTest.F_1_7;
      compilerVersion = CompilerOptions.VERSION_1_7;
    } else if (!"1.4".equals(compliance) && !"1.3".equals(compliance)) {
      throw new UnsupportedOperationException("Test framework doesn't support compliance level: " + compliance);
    }
    if (requiredComplianceFlag != 0) {
      if ((AbstractCompilerTest.getPossibleComplianceLevels() & requiredComplianceFlag) == 0)
        throw new RuntimeException("This test requires a " + compliance + " JRE");
      IJavaProject javaProject = JavaCore.create(project);
      Map options = new HashMap();
      options.put(CompilerOptions.OPTION_Compliance, compilerVersion);
      options.put(CompilerOptions.OPTION_Source, compilerVersion);
      options.put(CompilerOptions.OPTION_TargetPlatform, compilerVersion);
      javaProject.setOptions(options);
    }
    return project.getFullPath();
  }

  /**
   * Batch builds the workspace. A workspace must be open.
   */
  public void fullBuild() {
    checkAssertion("a workspace must be open", this.isOpen); //$NON-NLS-1$
    try {
      getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, null);
    } catch (CoreException e) {
      handle(e);
    }
  }

  /**
   * Batch builds a project. A workspace must be open.
   */
  public void fullBuild(IPath projectPath) {
    fullBuild(projectPath.lastSegment());
  }

  /**
   * Batch builds a project. A workspace must be open.
   */
  public void fullBuild(String projectName) {
    checkAssertion("a workspace must be open", this.isOpen); //$NON-NLS-1$
    try {
      getProject(projectName).build(IncrementalProjectBuilder.FULL_BUILD, null);
    } catch (CoreException e) {
      handle(e);
    }
  }

  public IWorkspace getWorkspace() {
    return this.workspace;
  }

}
