/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package dacapo.eclipse;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.tests.builder.TestingEnvironment;
import org.eclipse.jdt.core.tests.util.Util;
import org.eclipse.jdt.internal.core.JavaModelManager;
import org.eclipse.jdt.internal.core.JarPackageFragmentRoot;
import org.eclipse.jdt.internal.core.search.indexing.IndexManager;

/**
 * This class is heavily based on 
 *  org.eclipse.jdt.core.tests.performance.FullSourceWorkspaceTests
 */
public class EclipseTests {
  // Final static variables
  final static boolean DEBUG = "true".equals(System.getProperty("debug"));
  
  // Workspace variables
  protected static TestingEnvironment env = null;
  protected static IJavaProject[] ALL_PROJECTS;
  
  // Index variables
  protected static IndexManager indexManager = JavaModelManager.getJavaModelManager().getIndexManager();
  
  
  protected static void initialize() {
    try {
      if (env == null) {
        env = new TestingEnvironment();
        env.openEmptyWorkspace();
        System.out.println("<setting up workspace...>");
        setUpFullSourceWorkspace();
      }
      // TODO else error 
    } catch (Exception e) {
      System.err.println("Error creating workspace!");
      e.printStackTrace();
    }
  }
  
  protected static void runtests(String[] args) {
    boolean all = true;
    boolean build = false, hierarchy = false, ast = false, complete = false, search = false;
    int level = 0;
    for (int i = 0; i < args.length; i++) {
      if (args[i].equals("-level") && i < args.length - 1) {
        level = Integer.parseInt(args[i+1]);
      } else if (args[i].equals("-build")) {
        build = true; all = false;
      } else if (args[i].equals("-hierarchy")) {
        hierarchy = true; all = false;
      } else if (args[i].equals("-ast")) {
        ast = true; all = false;
      } else if (args[i].equals("-complete")) {
        complete = true; all = false;
      } else if (args[i].equals("-search")) {
        search = true; all = false;
      } 
    }
    initialize();
    System.out.println("<running tests at level "+level+"...>");
    try {
      if (build || all) { // Build tests
        System.out.println("<performing build tests...>");
        new EclipseBuildTests().doTests(level);
      }
      if (hierarchy || all) {	// Type hierarchy tests
        System.out.println("<performing type hierarchy tests...>");
        new EclipseTypeHierarchyTests().doTests(level);
      }
      if (ast || all) { // AST tests
        System.out.println("<performing AST tests...>");
        new EclipseASTTests().doTests(level);
      }
      if (complete || all) { // Completion tests
        System.out.println("<performing completion tests...>");
        new EclipseCompletionTests().doTests(level);
      }
      if (search || all) { // Search tests
        System.out.println("<performing search tests...>");
        new EclipseSearchTests().doTests(level);
      }
    } catch (Exception e) {
      System.err.println("Exception while running benchmarks!"+e);
      e.printStackTrace();
    }
    
    /* we've finished the tests, so kill off the environment */
    try {
      int length = ALL_PROJECTS.length;
      for (int i = 0; i < length; i++) {
        ALL_PROJECTS[i].getProject().delete(false, true, null);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    env = null;
  }
  
  /*
   * Set up full source workpsace (from zip file if necessary)
   */
  private static void setUpFullSourceWorkspace() throws IOException, CoreException {
    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    final IWorkspaceRoot workspaceRoot = workspace.getRoot();
    if (workspaceRoot.getProjects().length == 0) {
      createWorkspaceProjects(workspace, workspaceRoot);
    }
    ALL_PROJECTS = JavaCore.create(workspaceRoot).getJavaProjects();
  }
  
  private static void unzipWorkspace(IWorkspace workspace, final IWorkspaceRoot workspaceRoot) throws IOException, CoreException {
    String fullSourceZipPath = getPluginDirectoryPath() + File.separator + "full-source-R3_0.zip";
    final String targetWorkspacePath = workspaceRoot.getLocation().toFile().getCanonicalPath();
    
    if (DEBUG) System.out.print("Unzipping "+fullSourceZipPath+"...");
    Util.unzip(fullSourceZipPath, targetWorkspacePath);
    if (DEBUG) System.out.println("done!");
    
    createWorkspaceProjects(workspace, workspaceRoot);
    
  }
  
  private static void createWorkspaceProjects(IWorkspace workspace, final IWorkspaceRoot workspaceRoot) throws IOException, CoreException {
    final String targetWorkspacePath = workspaceRoot.getLocation().toFile().getCanonicalPath();
    
    if (DEBUG) System.out.print("Creating projects...");
    System.out.print("<creating projects");
    workspace.run(new IWorkspaceRunnable() {
      public void run(IProgressMonitor monitor) throws CoreException {
        File targetWorkspaceDir = new File(targetWorkspacePath);
        String[] projectNames = targetWorkspaceDir.list();
        for (int i = 0, length = projectNames.length; i < length; i++) {
          String projectName = projectNames[i];
          if (!".metadata".equals(projectName)) {
            System.out.print(".");
            //System.out.println(projectName);
            IProject project = workspaceRoot.getProject(projectName);
            project.create(monitor);
            project.open(monitor);
	  }
        }
        System.out.println(">");
      }
    }, null);
    if (DEBUG) System.out.println("done!");
  }
  
  
  /*
   * Returns the OS path to the directory that contains this plugin.
   */
  private static String getPluginDirectoryPath() {
    try {
      URL platformURL = Platform.getBundle("org.eclipse.jdt.core.tests.performance").getEntry("/");
      return new File(Platform.asLocalURL(platformURL).getFile()).getAbsolutePath();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }
  
  /**
   * Returns compilation unit with given name in given project and package.
   * @param projectName
   * @param packageName
   * @param unitName
   * @return org.eclipse.jdt.core.ICompilationUnit
   */
  protected ICompilationUnit getCompilationUnit(String projectName, String packageName, String unitName) throws JavaModelException {
    IJavaProject javaProject = getProject(projectName);
    if (javaProject == null) return null;
    IPackageFragmentRoot[] fragmentRoots = javaProject.getPackageFragmentRoots();
    int length = fragmentRoots.length;
    for (int i=0; i<length; i++) {
      if (fragmentRoots[i] instanceof JarPackageFragmentRoot) continue;
      IJavaElement[] packages= fragmentRoots[i].getChildren();
      for (int k= 0; k < packages.length; k++) {
        IPackageFragment pack = (IPackageFragment) packages[k];
        if (pack.getElementName().equals(packageName)) {
          ICompilationUnit[] units = pack.getCompilationUnits();
          for (int u=0; u<units.length; u++) {
            if (units[u].getElementName().equals(unitName))
              return units[u];
          }
        }
      }
    }
    return null;
  }
  
  /**
   * Returns project correspoding to given name or null if none is found.
   * @param projectName
   * @return IJavaProject
   */
  protected IJavaProject getProject(String projectName) {
    for (int i=0, length = ALL_PROJECTS.length; i<length; i++) {
      if (ALL_PROJECTS[i].getElementName().equals(projectName))
        return ALL_PROJECTS[i];
    }
    return null;
  }
  
  /**
   * Returns all compilation units of a given project.
   * @param javaProject Project to collect units
   * @return List of org.eclipse.jdt.core.ICompilationUnit
   */
  protected List getProjectCompilationUnits(IJavaProject javaProject) throws JavaModelException {
    IPackageFragmentRoot[] fragmentRoots = javaProject.getPackageFragmentRoots();
    int length = fragmentRoots.length;
    List allUnits = new ArrayList();
    for (int i=0; i<length; i++) {
      if (fragmentRoots[i] instanceof JarPackageFragmentRoot) continue;
      IJavaElement[] packages= fragmentRoots[i].getChildren();
      for (int k= 0; k < packages.length; k++) {
        IPackageFragment pack = (IPackageFragment) packages[k];
        ICompilationUnit[] units = pack.getCompilationUnits();
        for (int u=0; u<units.length; u++) {
          allUnits.add(units[u]);
        }
      }
    }
    return allUnits;
  }
  
  /**
   * Split a list of compilation units in several arrays.
   * @param units List of org.eclipse.jdt.core.ICompilationUnit
   * @param splitSize Size of the arrays
   * @return List of ICompilationUnit[]
   */
  protected List splitListInSmallArrays(List units, int splitSize) throws JavaModelException {
    int size = units.size();
    if (size == 0) return Collections.EMPTY_LIST;
    int length = size / splitSize;
    int remind = size%splitSize;
    List splitted = new ArrayList(remind==0?length:length+1);
    if (length == 0) {
      ICompilationUnit[] sublist = new ICompilationUnit[size];
      units.toArray(sublist);
      splitted.add(sublist);
      return splitted;
    }
    int ptr = 0;
    for (int i= 0; i<length; i++){
      ICompilationUnit[] sublist = new ICompilationUnit[splitSize];
      units.subList(ptr, ptr+splitSize).toArray(sublist);
      splitted.add(sublist);
      ptr += splitSize;
    }
    if (remind > 0) {
      if (remind< 10) {
        ICompilationUnit[] lastList = (ICompilationUnit[]) splitted.remove(length-1);
        System.arraycopy(lastList, 0, lastList = new ICompilationUnit[splitSize+remind], 0, splitSize);
        for (int i=ptr, j=splitSize; i<size; i++, j++) {
          lastList[j] = (ICompilationUnit) units.get(i);
        }
        splitted.add(lastList);
      } else {
        ICompilationUnit[] sublist = new ICompilationUnit[remind];
        units.subList(ptr, size).toArray(sublist);
        splitted.add(sublist);
      }
    }
    return splitted;
  }
}
