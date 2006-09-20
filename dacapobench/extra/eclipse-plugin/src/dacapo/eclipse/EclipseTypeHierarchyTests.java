package dacapo.eclipse;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;

/**
 * This class is heavily based on 
 *  org.eclipse.jdt.core.tests.performance.FullSourceWorkspaceTests and
 *  org.eclipse.jdt.core.tests.performance.FullSourceWorkspaceTypeHierarchyTests
 */
public class EclipseTypeHierarchyTests extends EclipseTests {
  
  void doTests(int level) throws CoreException {
    allTypes("org.eclipse.help","org.eclipse.help.internal","HelpPlugin");
    if (level > 0) {
      allTypes("org.eclipse.help","org.eclipse.help","HelpSystem");
      allTypes("org.eclipse.ant.core","org.eclipse.ant.core","AntRunner");
      if (level > 1) {
        allTypes("org.eclipse.jdt.core","org.eclipse.jdt.internal.compiler.ast","ASTNode");
      }
    }
  }
  
  private void allTypes(String projectName, String packageName, String unitName) throws CoreException {
    System.out.println("\tHierarchy: "+packageName+" "+unitName);
    ICompilationUnit unit = getCompilationUnit(projectName, packageName, unitName+".java");
    unit.getType(unitName).newTypeHierarchy(null).getAllClasses();
  }
}
