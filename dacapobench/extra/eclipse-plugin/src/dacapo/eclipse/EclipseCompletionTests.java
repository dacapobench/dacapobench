package dacapo.eclipse;

import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.CompletionRequestor;
import org.eclipse.jdt.core.CompletionProposal;

/**
 * This class is heavily based on 
 *  org.eclipse.jdt.core.tests.performance.FullSourceWorkspaceTests and
 *  org.eclipse.jdt.core.tests.performance.FullSourceWorkspaceCompletionTests
 */
public class EclipseCompletionTests extends EclipseTests {
  int proposalCount = 0;
  
  void doTests(int level) throws JavaModelException {
    completeEmptyName();
    completeEmptyNameWithoutMethods();
    if (level > 0) {
      completeEmptyNameWithoutTypes();
      completeMemberAccess();
      if (level > 1) {
        completeMethodDeclaration();
        completeName();
        completeNameWithoutMethods();
        completeNameWithoutTypes();
        completeTypeReference();
      }
    }
  }
  
  private class TestCompletionRequestor extends CompletionRequestor {
    public void accept(CompletionProposal proposal) {
      proposalCount++;
    }
  }
  
  private void complete(
      String testName,
      String projectName,
      String packageName,
      String unitName,
      String completeAt,
      String completeBehind) throws JavaModelException {
    this.complete(
        testName,
        projectName,
        packageName,
        unitName,
        completeAt,
        completeBehind,
        null);
  }
  private void complete(
      String testName,
      String projectName,
      String packageName,
      String unitName,
      String completeAt,
      String completeBehind,
      int[] ignoredKinds) throws JavaModelException {
    System.out.println("\tCompletion: "+testName);
    
    EclipseIndexTests.waitUntilIndexesReady();
    
    TestCompletionRequestor requestor = new TestCompletionRequestor();
    if(ignoredKinds != null) {
      for (int i = 0; i < ignoredKinds.length; i++) {
        requestor.setIgnored(ignoredKinds[i], true);
      }
    }
    
    ICompilationUnit unit =
      getCompilationUnit(projectName, packageName, unitName);
    
    String str = unit.getSource();
    int completionIndex = str.indexOf(completeAt) + completeBehind.length();
    
    if (DEBUG) System.out.print("Perform code assist inside " + unitName + "...");
    
    unit.codeComplete(completionIndex, requestor);
    
    if (DEBUG) System.out.println("done!");
  }
  
  void completeMethodDeclaration() throws JavaModelException {
    this.complete(
        "Completion>Method>Declaration",
        "org.eclipse.jdt.core",
        "org.eclipse.jdt.internal.core",
        "SourceType.java",
        "IType {",
    "IType {");
  }
  void completeMemberAccess() throws JavaModelException {
    this.complete(
        "Completion>Member>Access",
        "org.eclipse.jdt.core",
        "org.eclipse.jdt.internal.core",
        "SourceType.java",
        "this.",
    "this.");
  }
  void completeTypeReference() throws JavaModelException {
    this.complete(
        "Completion>Type>Reference",
        "org.eclipse.jdt.core",
        "org.eclipse.jdt.internal.core",
        "SourceType.java",
        "ArrayList list",
    "A");
  }
  void completeEmptyName() throws JavaModelException {
    this.complete(
        "Completion>Name>Empty",
        "org.eclipse.jdt.core",
        "org.eclipse.jdt.internal.core",
        "SourceType.java",
        "params.add",
    "");
  }
  void completeName() throws JavaModelException {
    this.complete(
        "Completion>Name",
        "org.eclipse.jdt.core",
        "org.eclipse.jdt.internal.core",
        "SourceType.java",
        "params.add",
    "p");
  }
  void completeEmptyNameWithoutTypes() throws JavaModelException {
    this.complete(
        "Completion>Name>Empty>No Type",
        "org.eclipse.jdt.core",
        "org.eclipse.jdt.internal.core",
        "SourceType.java",
        "params.add",
        "",
        new int[]{CompletionProposal.TYPE_REF});
  }
  void completeNameWithoutTypes() throws JavaModelException {
    this.complete(
        "Completion>Name>No Type",
        "org.eclipse.jdt.core",
        "org.eclipse.jdt.internal.core",
        "SourceType.java",
        "params.add",
        "p",
        new int[]{CompletionProposal.TYPE_REF});
  }
  void completeEmptyNameWithoutMethods() throws JavaModelException {
    this.complete(
        "Completion>Name>Empty>No Method",
        "org.eclipse.jdt.core",
        "org.eclipse.jdt.internal.core",
        "SourceType.java",
        "params.add",
        "",
        new int[]{CompletionProposal.METHOD_REF});
  }
  void completeNameWithoutMethods() throws JavaModelException {
    this.complete(
        "Completion>Name>No Method",
        "org.eclipse.jdt.core",
        "org.eclipse.jdt.internal.core",
        "SourceType.java",
        "params.add",
        "p",
        new int[]{CompletionProposal.METHOD_REF});
  }
  
}
