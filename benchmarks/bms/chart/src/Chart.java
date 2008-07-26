/**
 * Stub class which exists <b>only</b> to facilitate whole program
 * static analysis on a per-benchmark basis.  See also the "split-deps"
 * ant build target, which is also provided to enable whole program
 * static analysis.
 * 
 * @author Eric Bodden
 */
public class Chart {
  public static void main(String args[]) throws Exception {
    // create dummy harness and invoke with dummy arguments
    (new org.dacapo.harness.Chart(null, null)).run(null, "");
  }
}
