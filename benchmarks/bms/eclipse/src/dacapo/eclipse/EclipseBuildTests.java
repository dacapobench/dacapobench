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
import java.util.List;
import java.util.ArrayList;
import java.util.Hashtable;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IJavaModelMarker;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;

/**
 * This class is heavily based on 
 *  org.eclipse.jdt.core.tests.performance.FullSourceWorkspaceTests, and
 *  org.eclipse.jdt.core.tests.performance.FullSourceWorkspaceBuildTests
 */
public class EclipseBuildTests extends EclipseTests {
  
  private static boolean VERIFY = true;
  
  void doTests(int level) throws CoreException, IOException {
    /* The build ordering here is sensitive to the respective project
     * dependencies, so if you need to prune the list, do it bottom up...
     */
    buildDefault(getProject("org.apache.ant"));
//  buildDefault(getProject("org.apache.lucene"));
    buildDefault(getProject("org.junit"));
    buildDefault(getProject("org.eclipse.osgi"));
    if (level > 1) {
      buildDefault(getProject("org.eclipse.core.runtime"));
      buildDefault(getProject("org.eclipse.update.configurator"));
      buildDefault(getProject("org.eclipse.core.runtime.compatibility"));
      buildDefault(getProject("org.eclipse.core.variables"));
      buildDefault(getProject("org.eclipse.core.expressions"));
      buildDefault(getProject("org.eclipse.core.resources"));
      buildDefault(getProject("org.eclipse.core.boot"));
      buildDefault(getProject("org.eclipse.ant.core"));
      buildDefault(getProject("org.eclipse.text"));
      buildDefault(getProject("org.eclipse.core.filebuffers"));
      buildDefault(getProject("org.eclipse.debug.core"));
      if (level > 2) {
        buildDefault(getProject("org.eclipse.help"));
        buildDefault(getProject("org.eclipse.swt"));
        buildDefault(getProject("org.eclipse.jface"));
        buildDefault(getProject("org.eclipse.jface.text"));
        if (level > 3) {
          buildDefault(getProject("org.eclipse.ui.workbench"));
          buildDefault(getProject("org.eclipse.ui"));
          buildDefault(getProject("org.eclipse.ui.workbench.texteditor"));
          buildDefault(getProject("org.eclipse.ui.console"));
          buildDefault(getProject("org.eclipse.ui.views"));
          buildDefault(getProject("org.eclipse.update.core"));
          buildDefault(getProject("org.eclipse.help.appserver"));
          buildDefault(getProject("org.eclipse.help.base"));
          buildDefault(getProject("org.eclipse.ui.forms"));
          buildDefault(getProject("org.eclipse.update.ui"));
          buildDefault(getProject("org.eclipse.ui.ide"));
          buildDefault(getProject("org.eclipse.ui.editors"));
          buildDefault(getProject("org.eclipse.debug.ui"));
          buildDefault(getProject("org.eclipse.team.core"));
          buildDefault(getProject("org.eclipse.jdt.core"));
          buildDefault(getProject("org.eclipse.jdt.debug"));
          buildDefault(getProject("org.eclipse.jdt.launching"));
          buildDefault(getProject("org.eclipse.compare"));
          buildDefault(getProject("org.eclipse.ltk.core.refactoring"));
          buildDefault(getProject("org.eclipse.search"));
          buildDefault(getProject("org.eclipse.ltk.ui.refactoring"));
          buildDefault(getProject("org.eclipse.jdt.ui"));
          buildDefault(getProject("org.eclipse.ui.externaltools"));
          buildDefault(getProject("org.eclipse.jdt.debug.ui"));
          buildDefault(getProject("org.eclipse.ant.ui"));
          buildDefault(null); // build everything not already built
        }
      }
    }
  }
  
  
  /**
   * Full build with no warning.
   * 
   * Not calling tagAsSummary means that this test is currently evaluated
   * before put it in builds performance results.
   * 
   * @throws CoreException
   * @throws IOException
   */
  void buildNoWarning(IJavaProject project) throws CoreException, IOException {
    startBuild(project, warningOptions(-1/*no warning*/), false);
  }
  
  /**
   * Full build with JavaCore default options.
   * 
   * @throws CoreException
   * @throws IOException
   */
  void buildDefault(IJavaProject project) throws CoreException, IOException {
    startBuild(project, warningOptions(0/*default warnings*/), false);
  }
  
  /**
   * Full build with all warnings.
   * 
   * Not calling tagAsSummary means that this test is currently evaluated
   * before put it in builds performance results.
   * 
   * @throws CoreException
   * @throws IOException
   * 
   */
  void buildAllWarnings(IJavaProject project) throws CoreException, IOException {
    startBuild(project, warningOptions(1/*all warnings*/), false);
  }
  
  /**
   * Start a build on workspace using given options.
   * @param options
   * @throws IOException
   * @throws CoreException
   */
  private void startBuild(IJavaProject project, Hashtable options, boolean noWarning) throws IOException, CoreException {
    if (DEBUG) System.out.print("\tstart build...");
    
    JavaCore.setOptions(options);
    
    if (project == null) {
      System.out.println("\tBuilding: full workspace");
      env.fullBuild();
    } else {
      System.out.print("\t"+project.toString());
      System.out.print(" opening");
      project.getProject().open(null);
      System.out.print(" cleaning");
      project.getProject().build(IncrementalProjectBuilder.CLEAN_BUILD, null);
      System.out.print(" building");
      project.getProject().build(IncrementalProjectBuilder.FULL_BUILD, null);
      System.out.println();
    }
    
    if (VERIFY) {
      // Verify markers
      IMarker[] markers = ResourcesPlugin.getWorkspace().getRoot().findMarkers(IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER, true, IResource.DEPTH_INFINITE);
      List resources = new ArrayList();
      List messages = new ArrayList();
      int warnings = 0;
      for (int i = 0, length = markers.length; i < length; i++) {
        IMarker marker = markers[i];
        switch (((Integer) marker.getAttribute(IMarker.SEVERITY)).intValue()) {
        case IMarker.SEVERITY_ERROR:
          resources.add(marker.getResource().getName());
          messages.add(marker.getAttribute(IMarker.MESSAGE));
          break;
        case IMarker.SEVERITY_WARNING:
          warnings++;
          if (noWarning) {
            resources.add(marker.getResource().getName());
            messages.add(marker.getAttribute(IMarker.MESSAGE));
          }
          break;
        }
      }
      
      // Assert result
      int size = messages.size();
      if (size > 0) {
        StringBuffer debugBuffer = new StringBuffer();
        for (int i=0; i<size; i++) {
          debugBuffer.append(resources.get(i));
          debugBuffer.append(":\n\t");
          debugBuffer.append(messages.get(i));
          debugBuffer.append('\n');
        }
        System.out.println("Unexpected ERROR marker(s):\n" + debugBuffer.toString());
        System.out.println("--------------------");
      }
    }
    if (DEBUG) System.out.println("done");
  }
  
  /*
   * Create hashtable of none or all warning options.
   * Possible kind:
   * 	-1: no options
   *  0: default options
   *  1: all options
   */
  protected static Hashtable warningOptions(int kind) {
    // Values
    Hashtable optionsMap = null;
    switch (kind) {
    case 0:
      optionsMap = JavaCore.getDefaultOptions();
      break;
    default:
      optionsMap = new Hashtable(350);
    break;
    }
    if (kind == 0) {
      // Default set since 3.1
      optionsMap.put(CompilerOptions.OPTION_ReportUnusedImport, CompilerOptions.IGNORE); 
    } else {
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
    
    // Return created options map
    return optionsMap;
  }
}
