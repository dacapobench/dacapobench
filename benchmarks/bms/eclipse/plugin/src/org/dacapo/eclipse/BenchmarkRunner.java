/*
 * Copyright (c) 2005, 2009 The Australian National University.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License v2.0.
 * You may obtain the license at
 * 
 *    http://www.opensource.org/licenses/apache2.0.php
 */
package org.dacapo.eclipse;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.jdt.core.tests.performance.FullSourceWorkspaceASTTests;
import org.eclipse.jdt.core.tests.performance.FullSourceWorkspaceBuildTests;
import org.eclipse.jdt.core.tests.performance.FullSourceWorkspaceCompleteSearchTests;
import org.eclipse.jdt.core.tests.performance.FullSourceWorkspaceCompletionTests;
import org.eclipse.jdt.core.tests.performance.FullSourceWorkspaceFormatterTests;
import org.eclipse.jdt.core.tests.performance.FullSourceWorkspaceModelTests;
import org.eclipse.jdt.core.tests.performance.FullSourceWorkspaceSearchTests;
import org.eclipse.jdt.core.tests.performance.FullSourceWorkspaceTests;
import org.eclipse.jdt.core.tests.performance.FullSourceWorkspaceTypeHierarchyTests;

/**
 * @date $Date: 2009-12-24 11:19:36 +1100 (Thu, 24 Dec 2009) $
 * @id $Id: BenchmarkRunner.java 738 2009-12-24 00:19:36Z steveb-oss $
 */
public class BenchmarkRunner implements IApplication {

  public Object start(IApplicationContext context) throws Exception {
    boolean large = false, unzip = false, setup = false, index = false, build = false, hierarchy = false, ast = false, complete = false, search = false, format = false, model = false, teardown = false;

    String[] args = (String[]) context.getArguments().get(IApplicationContext.APPLICATION_ARGS);
    for (int i = 0; i < args.length; i++) {
      String arg = args[i];
      if (arg.equals("large")) {
        large = true;
      } else if (arg.equals("unzip")) {
        unzip = true;
      } else if (arg.equals("setup")) {
        setup = true;
      } else if (arg.equals("index")) {
        index = true;
      } else if (arg.equals("build")) {
        build = true;
      } else if (arg.equals("hierarchy")) {
        hierarchy = true;
      } else if (arg.equals("ast")) {
        ast = true;
      } else if (arg.equals("complete")) {
        complete = true;
      } else if (arg.equals("search")) {
        search = true;
      } else if (arg.equals("format")) {
        format = true;
      } else if (arg.equals("model")) {
        model = true;
      } else if (arg.equals("teardown")) {
        teardown = true;
      } else if (arg.equals("all")) {
        unzip = setup = index = search = build = hierarchy = ast = complete = format = model = teardown = true;
      } else if (arg.equals("alltests")) {
        index = search = build = hierarchy = ast = complete = format = model = true;
      }
    }

    if (unzip)     { FullSourceWorkspaceTests.unzipWorkSpace(large); }
    if (setup)     { FullSourceWorkspaceTests.setup(large); }
    
    if (index)     { FullSourceWorkspaceSearchTests.runDaCapoTests(); }
    if (build)     { FullSourceWorkspaceBuildTests.runDaCapoTests();  }
    if (search)    { FullSourceWorkspaceCompleteSearchTests.runDaCapoTests(); }
    if (hierarchy) { FullSourceWorkspaceTypeHierarchyTests.runDaCapoTests(); }
    if (ast)       { FullSourceWorkspaceASTTests.runDaCapoTests();}
    if (complete)  { FullSourceWorkspaceCompletionTests.runDaCapoTests(); }
    if (format)    { FullSourceWorkspaceFormatterTests.runDaCapoTests(); }
    if (model)     { FullSourceWorkspaceModelTests.runDaCapoTests(); }
    
    if (teardown)  { FullSourceWorkspaceTests.tearDown(); }
    
    return EXIT_OK;
  }

  public void stop() {
    // TODO Auto-generated method stub
  }

}
