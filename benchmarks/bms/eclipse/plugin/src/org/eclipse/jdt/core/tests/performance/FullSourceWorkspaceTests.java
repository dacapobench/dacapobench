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

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.core.JarPackageFragmentRoot;
import org.eclipse.jdt.internal.core.JavaModelManager;
import org.eclipse.jdt.internal.core.search.indexing.IndexManager;

import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.tests.builder.TestingEnvironment;
import org.eclipse.jdt.core.tests.util.Util;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;

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
 * @id $Id: FullSourceWorkspaceTests.java 738 2009-12-24 00:19:36Z steveb-oss $
 */
public abstract class FullSourceWorkspaceTests {

  // Debug variables
  final static boolean DEBUG = "true".equals(System.getProperty("debug"));
  final static boolean PRINT = "true".equals(System.getProperty("print"));
  final static boolean DACAPO_PRINT = true;

  final static Hashtable INITIAL_OPTIONS = JavaCore.getOptions();
  protected static TestingEnvironment ENV = null;
  protected static IJavaProject[] ALL_PROJECTS;
  protected static IJavaProject JDT_CORE_PROJECT;
  protected static ICompilationUnit PARSER_WORKING_COPY;
  protected final static String BIG_PROJECT_NAME = "BigProject";
  protected final static String FULL_SOURCE_ZIP_REL_PATH = "full-source-R3_0"; // path
  // to
  // source
  // relative
  // to
  // parent
  // of
  // workspace
  final static String COMPLIANCE = System.getProperty("compliance");

  // Index variables
  protected static IndexManager INDEX_MANAGER = JavaModelManager.getIndexManager();

  public static final boolean VERBOSE = false;

  protected static String compliance() {
    String compliance = null;
    if ("1.3".equals(COMPLIANCE)) {
      compliance = CompilerOptions.VERSION_1_3;
    } else if ("1.4".equals(COMPLIANCE)) {
      compliance = CompilerOptions.VERSION_1_4;
    } else if ("1.5".equals(COMPLIANCE) || "5.0".equals(COMPLIANCE)) {
      compliance = CompilerOptions.VERSION_1_5;
    } else if ("1.6".equals(COMPLIANCE) || "6.0".equals(COMPLIANCE)) {
      compliance = CompilerOptions.VERSION_1_6;
    }
    return compliance;
  }

  public static void setup(boolean large) {
    if (VERBOSE)
      System.err.println("setup()");
    try {
      // Set testing environment if null
      if (ENV == null) {
        if (DACAPO_PRINT)
          System.out.print("Initialize workspace ");
        ENV = new TestingEnvironment();
        ENV.openEmptyWorkspace();
        setUpFullSourceWorkspace(large);
        if (DACAPO_PRINT)
          System.out.println();
      }
    } catch (Exception e) {
      System.err.println("Error creating workspace!");
      e.printStackTrace();
    }
  }

  public static void unzipWorkSpace(boolean large) {
    if (VERBOSE)
      System.err.println("unzipWorkSpace()");
    if (DACAPO_PRINT)
      System.out.print("Unzip workspace ");
    try {
      final IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
      File targetWorkspaceDir = workspaceRoot.getLocation().toFile();
      String targetWorkspacePath = targetWorkspaceDir.getCanonicalPath();
      deleteWorkspace();
      String fullSourceZipPath = targetWorkspaceDir.getParent() + File.separator + FULL_SOURCE_ZIP_REL_PATH;
      unzip(fullSourceZipPath + "-default.zip", targetWorkspacePath);
      if (large) {
        unzip(fullSourceZipPath + "-large.zip", targetWorkspacePath);
      }
    } catch (IOException e) {
      System.err.println("Error creating workspace!");
      e.printStackTrace();
    }
    if (DACAPO_PRINT)
      System.out.println();
  }

  private static void unzip(String sourceZipPath, String targetWorkspacePath) throws IOException {
    if (VERBOSE) {
      System.err.println("Unzipping " + sourceZipPath);
      System.err.println("       in " + targetWorkspacePath + "...");
    }
    Util.unzip(sourceZipPath, targetWorkspacePath);
  }

  public static void tearDown() {
    if (DACAPO_PRINT)
      System.out.print("Delete workspace ");
    if (VERBOSE)
      System.err.println("close()");
    ENV.close();
    ENV = null;
    deleteWorkspace();
    JavaCore.setOptions(INITIAL_OPTIONS);
    if (DACAPO_PRINT)
      System.out.println();
  }

  private static void deleteWorkspace() {
    try {
      ResourcesPlugin.getWorkspace().getRoot().delete(true, null);
      ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile().delete();
    } catch (Exception e) {
      System.err.println("Problem deleting workspace");
      e.printStackTrace();
    }
  }

  /*
   * Clear given options
   */
  Map clearOptions(Map options) {
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
        // System.out.println(" - disabling " + key);
        options.put(key, "disabled");
      }
    }
    options.put(JavaCore.COMPILER_TASK_TAGS, "");
    return options;
  }

  /**
   * Returns the specified compilation unit in the given project, root, and
   * package fragment or <code>null</code> if it does not exist.
   */
  protected IClassFile getClassFile(IJavaProject project, String rootPath, String packageName, String className) throws JavaModelException {
    IPackageFragment pkg = getPackageFragment(project, rootPath, packageName);
    if (pkg == null) {
      return null;
    }
    return pkg.getClassFile(className);
  }

  /**
   * Returns compilation unit with given name in given project and package.
   * @param projectName
   * @param packageName
   * @param unitName
   * @return org.eclipse.jdt.core.ICompilationUnit
   */
  protected ICompilationUnit getCompilationUnit(String projectName, String packageName, String unitName) throws CoreException {
    IJavaProject javaProject = getProject(projectName);
    if (javaProject == null)
      return null;
    IType type = javaProject.findType(packageName, unitName);
    if (type != null) {
      return type.getCompilationUnit();
    }
    return null;
  }

  /**
   * Returns the specified package fragment in the given project and root, or
   * <code>null</code> if it does not exist. The rootPath must be specified as a
   * project relative path. The empty path refers to the default package
   * fragment.
   */
  protected IPackageFragment getPackageFragment(IJavaProject project, String rootPath, String packageName) throws JavaModelException {
    IPackageFragmentRoot root = getPackageFragmentRoot(project, rootPath);
    if (root == null) {
      return null;
    }
    return root.getPackageFragment(packageName);
  }

  /**
   * Returns the specified package fragment root in the given project, or
   * <code>null</code> if it does not exist. If relative, the rootPath must be
   * specified as a project relative path. The empty path refers to the package
   * fragment root that is the project folder iteslf. If absolute, the rootPath
   * refers to either an external jar, or a resource internal to the workspace
   */
  public IPackageFragmentRoot getPackageFragmentRoot(IJavaProject project, String rootPath) throws JavaModelException {

    if (project == null) {
      return null;
    }
    IPath path = new Path(rootPath);
    if (path.isAbsolute()) {
      IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
      IResource resource = workspaceRoot.findMember(path);
      IPackageFragmentRoot root;
      if (resource == null) {
        // external jar
        root = project.getPackageFragmentRoot(rootPath);
      } else {
        // resource in the workspace
        root = project.getPackageFragmentRoot(resource);
      }
      return root;
    } else {
      IPackageFragmentRoot[] roots = project.getPackageFragmentRoots();
      if (roots == null || roots.length == 0) {
        return null;
      }
      for (int i = 0; i < roots.length; i++) {
        IPackageFragmentRoot root = roots[i];
        if (!root.isExternal() && root.getUnderlyingResource().getProjectRelativePath().equals(path)) {
          return root;
        }
      }
    }
    return getExternalJarFile(project, rootPath);
  }

  protected IPackageFragmentRoot getExternalJarFile(IJavaProject project, String jarSimpleName) throws JavaModelException {
    IPackageFragmentRoot[] roots = project.getPackageFragmentRoots();
    if (roots == null || roots.length == 0) {
      return null;
    }
    for (int i = 0; i < roots.length; i++) {
      IPackageFragmentRoot root = roots[i];
      if (root.isExternal() && root.getElementName().equals(jarSimpleName)) {
        return root;
      }
    }
    return null;
  }

  /**
   * Returns project corresponding to given name or null if none is found.
   * @param projectName
   * @return IJavaProject
   */
  protected IJavaProject getProject(String projectName) {
    for (int i = 0, length = ALL_PROJECTS.length; i < length; i++) {
      if (ALL_PROJECTS[i].getElementName().equals(projectName))
        return ALL_PROJECTS[i];
    }
    return null;
  }

  static void setUpFullSourceWorkspace(boolean large) throws Exception {
    // Get wksp info
    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    final IWorkspaceRoot workspaceRoot = workspace.getRoot();
    String targetWorkspacePath = workspaceRoot.getLocation().toFile().getCanonicalPath();

    // Modify resources workspace preferences to avoid disturbing tests while
    // running them
    IEclipsePreferences resourcesPreferences = new InstanceScope().getNode(ResourcesPlugin.PI_RESOURCES);
    resourcesPreferences.put(ResourcesPlugin.PREF_AUTO_REFRESH, "false");
    workspace.getDescription().setSnapshotInterval(Long.MAX_VALUE);
    workspace.getDescription().setAutoBuilding(false);

    // Get projects directories
    File wkspDir = new File(targetWorkspacePath);
    FullSourceProjectsFilter filter = new FullSourceProjectsFilter();
    File[] directories = wkspDir.listFiles(filter);
    int dirLength = directories == null ? 0 : directories.length;
    if (dirLength < 5) {
      System.out.println("Workspace empty!");
      unzipWorkSpace(large);
    }
    for (int i = 0; i < dirLength; i++) {
      String dirName = directories[i].getName();
      IProject project = workspaceRoot.getProject(dirName);
      if (project.exists()) {
        if (DACAPO_PRINT)
          System.out.print("o");
        ENV.addProject(project);
      } else {
        if (DACAPO_PRINT)
          System.out.print(".");
        ENV.addProject(dirName);
      }
    }

    // Create lib entries for the JDKs
    String jreLibPath = JavaCore.getClasspathVariable("JRE_LIB").toOSString();
    String[] jdkLibs = Util.getJavaClassLibs();
    int jdkLibsLength = jdkLibs.length;
    IClasspathEntry[] jdkEntries = new IClasspathEntry[jdkLibsLength];
    int jdkEntriesCount = 0;
    for (int i = 0; i < jdkLibsLength; i++) {
      if (!jdkLibs[i].equals(jreLibPath)) { // do not include JRE_LIB in
        // additional JDK entries
        jdkEntries[jdkEntriesCount++] = JavaCore.newLibraryEntry(new Path(jdkLibs[i]), null, null);
      }
    }

    // Set classpaths (workaround bug 73253 Project references not set on
    // project open)
    ALL_PROJECTS = JavaCore.create(workspaceRoot).getJavaProjects();
    int projectsLength = ALL_PROJECTS.length;
    for (int i = 0; i < projectsLength; i++) {
      String projectName = ALL_PROJECTS[i].getElementName();
      if (BIG_PROJECT_NAME.equals(projectName))
        continue; // will be set later
      if (JavaCore.PLUGIN_ID.equals(projectName)) {
        JDT_CORE_PROJECT = ALL_PROJECTS[i];
        // } else if (JUNIT_PROJECT_NAME.equals(projectName)) {
        // JUNIT_PROJECT = ALL_PROJECTS[i];
      }

      // Set jdk jars onto the project classpath
      IClasspathEntry[] entries = ALL_PROJECTS[i].getRawClasspath();
      int entriesLength = entries.length;
      try {
        System.arraycopy(entries, 0, entries = new IClasspathEntry[jdkEntriesCount + entriesLength], 0, entriesLength);
        System.arraycopy(jdkEntries, 0, entries, entriesLength, jdkEntriesCount);
        ALL_PROJECTS[i].setRawClasspath(entries, null);
      } catch (CoreException jme) {
        // skip name collision as it means that JRE lib were already set on the
        // classpath
        if (jme.getStatus().getCode() != IJavaModelStatusConstants.NAME_COLLISION) {
          throw jme;
        }
      }
    }
  }

  // Filter to get only the 3.0 plugins
  static class FullSourceProjectsFilter implements FileFilter {
    public boolean accept(File project) {
      if (project.isDirectory()) {
        StringTokenizer tokenizer = new StringTokenizer(project.getName(), ".");
        String token = tokenizer.nextToken();
        if (token.equals("org") && tokenizer.hasMoreTokens()) {
          token = tokenizer.nextToken();
          if (token.equals("junit") && !tokenizer.hasMoreTokens()) {
            return true;
          }
          if (token.equals("apache")) {
            token = tokenizer.nextToken();
            if (token.equals("ant") || token.equals("lucene")) {
              return true;
            }
            return false;
          }
          if (token.equals("eclipse") && tokenizer.hasMoreTokens()) {
            return true;
            /*
             * token = tokenizer.nextToken(); if (token.equals("core") ||
             * token.equals("osgi") || token.equals("text")) { return true; }
             * else if (token.equals("jdt")) { token = tokenizer.nextToken(); if
             * (token.equals("core")) { return true; } } else if
             * (token.equals("team")) { token = tokenizer.nextToken(); if
             * (token.equals("core")) { return true; } } else if
             * (token.equals("update")) { token = tokenizer.nextToken(); if
             * (token.equals("configurator")) { return true; } } return false;
             */
          }
        }
      }
      return false;
    }
  }

  /**
   * Returns all compilation units of a given project.
   * @param javaProject Project to collect units
   * @return List of org.eclipse.jdt.core.ICompilationUnit
   */
  protected List getProjectCompilationUnits(IJavaProject javaProject) throws CoreException {
    IPackageFragmentRoot[] fragmentRoots = javaProject.getPackageFragmentRoots();
    int length = fragmentRoots.length;
    List allUnits = new ArrayList();
    for (int i = 0; i < length; i++) {
      if (fragmentRoots[i] instanceof JarPackageFragmentRoot)
        continue;
      IJavaElement[] packages = fragmentRoots[i].getChildren();
      for (int k = 0; k < packages.length; k++) {
        IPackageFragment pack = (IPackageFragment) packages[k];
        ICompilationUnit[] units = pack.getCompilationUnits();
        for (int u = 0; u < units.length; u++) {
          allUnits.add(units[u]);
        }
      }
    }
    return allUnits;
  }

  /*
   * Create hashtable of none or all warning options. Possible kind: -1: no
   * options 0: default options 1: all options
   */
  protected Hashtable warningOptions(int kind) {

    // Values
    Hashtable optionsMap = JavaCore.getDefaultOptions();
    if (kind == 0) {
      // Default set since 3.1
      optionsMap.put(CompilerOptions.OPTION_ReportUnusedImport, CompilerOptions.IGNORE);
    } else {
      clearOptions(optionsMap);
      boolean all = kind == 1;
      String generate = all ? CompilerOptions.GENERATE : CompilerOptions.DO_NOT_GENERATE;
      String warning = all ? CompilerOptions.WARNING : CompilerOptions.IGNORE;
      String enabled = all ? CompilerOptions.ENABLED : CompilerOptions.DISABLED;
      String preserve = all ? CompilerOptions.OPTIMIZE_OUT : CompilerOptions.PRESERVE;

      // Set options values
      optionsMap.put(CompilerOptions.OPTION_LocalVariableAttribute, generate);
      optionsMap.put(CompilerOptions.OPTION_LineNumberAttribute, generate);
      optionsMap.put(CompilerOptions.OPTION_SourceFileAttribute, generate);
      optionsMap.put(CompilerOptions.OPTION_PreserveUnusedLocal, preserve);
      optionsMap.put(CompilerOptions.OPTION_DocCommentSupport, enabled);
      optionsMap.put(CompilerOptions.OPTION_ReportMethodWithConstructorName, warning);
      optionsMap.put(CompilerOptions.OPTION_ReportOverridingPackageDefaultMethod, warning);
      optionsMap.put(CompilerOptions.OPTION_ReportDeprecation, warning);
      optionsMap.put(CompilerOptions.OPTION_ReportDeprecationInDeprecatedCode, enabled);
      optionsMap.put(CompilerOptions.OPTION_ReportDeprecationWhenOverridingDeprecatedMethod, enabled);
      optionsMap.put(CompilerOptions.OPTION_ReportHiddenCatchBlock, warning);
      optionsMap.put(CompilerOptions.OPTION_ReportUnusedLocal, warning);
      optionsMap.put(CompilerOptions.OPTION_ReportUnusedParameter, warning);
      optionsMap.put(CompilerOptions.OPTION_ReportUnusedImport, warning);
      optionsMap.put(CompilerOptions.OPTION_ReportSyntheticAccessEmulation, warning);
      optionsMap.put(CompilerOptions.OPTION_ReportNoEffectAssignment, warning);
      optionsMap.put(CompilerOptions.OPTION_ReportNonExternalizedStringLiteral, warning);
      optionsMap.put(CompilerOptions.OPTION_ReportNoImplicitStringConversion, warning);
      optionsMap.put(CompilerOptions.OPTION_ReportNonStaticAccessToStatic, warning);
      optionsMap.put(CompilerOptions.OPTION_ReportIndirectStaticAccess, warning);
      optionsMap.put(CompilerOptions.OPTION_ReportIncompatibleNonInheritedInterfaceMethod, warning);
      optionsMap.put(CompilerOptions.OPTION_ReportUnusedPrivateMember, warning);
      optionsMap.put(CompilerOptions.OPTION_ReportLocalVariableHiding, warning);
      optionsMap.put(CompilerOptions.OPTION_ReportFieldHiding, warning);
      optionsMap.put(CompilerOptions.OPTION_ReportPossibleAccidentalBooleanAssignment, warning);
      optionsMap.put(CompilerOptions.OPTION_ReportEmptyStatement, warning);
      optionsMap.put(CompilerOptions.OPTION_ReportAssertIdentifier, warning);
      optionsMap.put(CompilerOptions.OPTION_ReportUndocumentedEmptyBlock, warning);
      optionsMap.put(CompilerOptions.OPTION_ReportUnnecessaryTypeCheck, warning);
      optionsMap.put(CompilerOptions.OPTION_ReportUnnecessaryElse, warning);
      optionsMap.put(CompilerOptions.OPTION_ReportInvalidJavadoc, warning);
      optionsMap.put(CompilerOptions.OPTION_ReportInvalidJavadocTags, enabled);
      optionsMap.put(CompilerOptions.OPTION_ReportMissingJavadocTags, warning);
      optionsMap.put(CompilerOptions.OPTION_ReportMissingJavadocComments, warning);
      optionsMap.put(CompilerOptions.OPTION_ReportFinallyBlockNotCompletingNormally, warning);
      optionsMap.put(CompilerOptions.OPTION_ReportUnusedDeclaredThrownException, warning);
      optionsMap.put(CompilerOptions.OPTION_ReportUnqualifiedFieldAccess, warning);
      optionsMap.put(CompilerOptions.OPTION_TaskTags, all ? JavaCore.DEFAULT_TASK_TAGS : "");
      optionsMap.put(CompilerOptions.OPTION_TaskPriorities, all ? JavaCore.DEFAULT_TASK_PRIORITIES : "");
      optionsMap.put(CompilerOptions.OPTION_TaskCaseSensitive, enabled);
      optionsMap.put(CompilerOptions.OPTION_ReportUnusedParameterWhenImplementingAbstract, enabled);
      optionsMap.put(CompilerOptions.OPTION_ReportUnusedParameterWhenOverridingConcrete, enabled);
      optionsMap.put(CompilerOptions.OPTION_ReportSpecialParameterHidingField, enabled);
      optionsMap.put(CompilerOptions.OPTION_InlineJsr, enabled);
    }

    // Ignore 3.1 options
    optionsMap.put(CompilerOptions.OPTION_ReportMissingSerialVersion, CompilerOptions.IGNORE);
    optionsMap.put(CompilerOptions.OPTION_ReportEnumIdentifier, CompilerOptions.IGNORE);

    // Ignore 3.2 options
    optionsMap.put(CompilerOptions.OPTION_ReportUnusedLabel, CompilerOptions.IGNORE);

    // Set compliance
    String compliance = compliance();
    if (compliance != null) {
      optionsMap.put(CompilerOptions.OPTION_Compliance, compliance);
      optionsMap.put(CompilerOptions.OPTION_Source, compliance);
      optionsMap.put(CompilerOptions.OPTION_TargetPlatform, compliance);
    }

    // Return created options map
    return optionsMap;
  }
}