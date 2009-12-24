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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaModelMarker;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

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
 * @id $Id: FullSourceWorkspaceBuildTests.java 738 2009-12-24 00:19:36Z steveb-oss $
 */
public class FullSourceWorkspaceBuildTests extends FullSourceWorkspaceTests {

  public static void runDaCapoTests() {
    try {
      FullSourceWorkspaceBuildTests b = new FullSourceWorkspaceBuildTests();
      if (DACAPO_PRINT)
        System.out.print("Build workspace ");
      b.testFullBuildDefault();
      if (DACAPO_PRINT)
        System.out.println();
    } catch (Exception e) {
      System.err.println("Caught exception performing build tests: ");
      e.printStackTrace();
    }
  }

  /**
   * Start a build on given project or workspace using given options.
   * 
   * @param javaProject Project which must be (full) build or null if all
   * workspace has to be built.
   * @param options Options used while building
   */
  void build(final IJavaProject javaProject, Hashtable options, boolean noWarning) throws IOException, CoreException {
    if (DEBUG)
      System.out.print("\tstart build...");
    JavaCore.setOptions(options);
    if (PRINT)
      System.out.println("JavaCore options: " + options);

    // Build workspace if no project
    if (javaProject == null) {
      // single measure
      ENV.fullBuild();
    } else {
      if (PRINT)
        System.out.println("Project options: " + javaProject.getOptions(false));
      IWorkspaceRunnable compilation = new IWorkspaceRunnable() {
        public void run(IProgressMonitor monitor) throws CoreException {
          ENV.fullBuild(javaProject.getPath());
        }
      };
      IWorkspace workspace = ResourcesPlugin.getWorkspace();
      workspace.run(compilation, null/* don't take any lock */, IWorkspace.AVOID_UPDATE, null/*
                                                                                              * no
                                                                                              * progress
                                                                                              * available
                                                                                              * here
                                                                                              */);
    }

    // Verify markers
    IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
    IMarker[] markers = workspaceRoot.findMarkers(IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER, true, IResource.DEPTH_INFINITE);
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
    workspaceRoot.deleteMarkers(IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER, true, IResource.DEPTH_INFINITE);

    // Assert result
    int size = messages.size();
    if (size > 0) {
      StringBuffer debugBuffer = new StringBuffer();
      for (int i = 0; i < size; i++) {
        debugBuffer.append(resources.get(i));
        debugBuffer.append(":\n\t");
        debugBuffer.append(messages.get(i));
        debugBuffer.append('\n');
      }
      System.out.println("Unexpected ERROR marker(s):\n" + debugBuffer.toString());
      System.out.println("--------------------");
      String target = javaProject == null ? "workspace" : javaProject.getElementName();
      // assertEquals("Found "+size+" unexpected errors while building "+target,
      // 0, size);
    }
    if (DEBUG)
      System.out.println("done");
  }

  /**
   * Full build with JavaCore default options.
   * 
   * WARNING: This test must be and _ever_ stay at first position as it build
   * the entire workspace. It also cannot be removed as it's a Global
   * fingerprint! Move it would have great consequence on all other tests
   * results...
   * 
   * @throws CoreException
   * @throws IOException
   */
  public void testFullBuildDefault() throws CoreException, IOException {
    build(null, warningOptions(0/* default warnings */), false);
  }
}
