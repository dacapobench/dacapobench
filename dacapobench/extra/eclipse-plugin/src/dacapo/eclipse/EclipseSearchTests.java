package dacapo.eclipse;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchRequestor;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchEngine;

/**
 * This class is heavily based on 
 *  org.eclipse.jdt.core.tests.performance.FullSourceWorkspaceTests, and
 *  org.eclipse.jdt.core.tests.performance.FullSourceWorkspaceSearchTests
 */
public class EclipseSearchTests extends EclipseTests implements IJavaSearchConstants {
  // Scopes
  IJavaSearchScope workspaceScope;
  protected JavaSearchResultCollector resultCollector;
  
  // Search stats
  private static int[] REFERENCES = new int[4];
  
  void doTests(int level) throws CoreException {
    System.out.println("\tSearching: indexing");
    indexing();
    if (level > 0) {
      System.out.println("\tSearching: constructor");
      searchConstructor();
      System.out.println("\tSearching: field");
      searchField();
      if (level > 1) {
        System.out.println("\tSearching: method");
        searchMethod();
        System.out.println("\tSearching: type");
        searchType();
      }
    }
  }
  
  /**
   * Constructor
   *
   */
  EclipseSearchTests() {
    this.resultCollector = new JavaSearchResultCollector();
    this.workspaceScope = SearchEngine.createWorkspaceScope();
  }
  
  /**
   * Performance tests for search: Indexing.
   * 
   * First wait that already started indexing jobs end before perform test.
   * Consider this initial indexing jobs as warm-up for this test.
   */
  void indexing() throws CoreException {
    // Wait for indexing end (we use initial indexing as warm-up)
    EclipseIndexTests.waitUntilIndexesReady();
    
    // Remove all previous indexing
    indexManager.removeIndexFamily(new Path(""));
    indexManager.reset();
    
    // Restart brand new indexing
    for (int i=0, length=ALL_PROJECTS.length; i<length; i++) {
      indexManager.indexAll(ALL_PROJECTS[i].getProject());
    }
    
    // Wait for indexing end
    EclipseIndexTests.waitUntilIndexesReady();
  }
  
  /**
   * Performance tests for search: Occurence Types.
   * 
   * First wait that already started indexing jobs end before perform test.
   * Perform one search before measure performance for warm-up.
   * 
   * Note that following search have been tested:
   *		- "String":				> 65000 macthes (CAUTION: needs -Xmx512M)
   *		- "Object":			13497 matches
   *		- ""IResource":	5886 macthes
   *		- "JavaCore":		2145 matches
   */
  void searchType() throws CoreException {
    // Wait for indexing end
    EclipseIndexTests.waitUntilIndexesReady();
    
    search("JavaCore", TYPE, ALL_OCCURRENCES);
    
    // Store counter
    REFERENCES[0] = this.resultCollector.count;
  }
  
  /**
   * Performance tests for search: Declarations Types Names.
   * 
   * First wait that already started indexing jobs end before perform test.
   * Perform one search before measure performance for warm-up.
   */
  void searchField() throws CoreException {
    // Wait for indexing end
    EclipseIndexTests.waitUntilIndexesReady();
    search("FILE", FIELD, ALL_OCCURRENCES);
    // Store counter
    REFERENCES[1] = this.resultCollector.count;
  }
  
  /**
   * Performance tests for search: Declarations Types Names.
   * 
   * First wait that already started indexing jobs end before perform test.
   * Perform one search before measure performance for warm-up.
   */
  void searchMethod() throws CoreException {
    search("equals", METHOD, ALL_OCCURRENCES);
    // Store counter
    REFERENCES[2] = this.resultCollector.count;
  }
  
  /**
   * Performance tests for search: Declarations Types Names.
   * 
   * First wait that already started indexing jobs end before perform test.
   * Perform one search before measure performance for warm-up.
   */
  void searchConstructor() throws CoreException {
    search("String", CONSTRUCTOR, ALL_OCCURRENCES);
    // Store counter
    REFERENCES[3] = this.resultCollector.count;
  }	 
  
  private void search(String patternString, int searchFor, int limitTo) throws CoreException {
    int matchMode = patternString.indexOf('*') != -1 || patternString.indexOf('?') != -1
    ? SearchPattern.R_PATTERN_MATCH
        : SearchPattern.R_EXACT_MATCH;
    SearchPattern pattern = SearchPattern.createPattern(
        patternString, 
        searchFor,
        limitTo, 
        matchMode | SearchPattern.R_CASE_SENSITIVE);
    new SearchEngine().search(
        pattern,
        new SearchParticipant[] {SearchEngine.getDefaultSearchParticipant()},
        this.workspaceScope,
        this.resultCollector,
        null);
  }
  
  /**
   * Simple search result collector: only count matches.
   */
  class JavaSearchResultCollector extends SearchRequestor {
    int count = 0;
    public void acceptSearchMatch(SearchMatch match) throws CoreException {
      this.count++;
    }
  }
}
