/**
 * Example usage:
 * 
 * java -javaagent:dacapo-chopin/jar/java-allocation-instrumenter-3.3.0.jar -jar dacapo-chopin.jar -callback org.dacapo.analysis.AllocCallback fop
 */
package org.dacapo.analysis;

import java.util.Map;
import java.util.TreeMap;
import org.dacapo.harness.Callback;
import org.dacapo.harness.CommandLineArgs;
import com.google.monitoring.runtime.instrumentation.AllocationRecorder;
import com.google.monitoring.runtime.instrumentation.Sampler;

public class AllocCallback extends Callback {

  private Boolean measuring = new Boolean(false);
  private long bytesAllocated = 0;
  private long objectsAllocated = 0;
  private Map<Long, Long> bytesBySize;
  private Map<Long, Long> objectsBySize;
  private Map<String, Long> bytesByType;
  private Map<String, Long> objectsByType;

  public AllocCallback(CommandLineArgs args) {
    super(args);
    AllocationRecorder.addSampler(new Sampler() {
      public void sampleAllocation(int count, String desc, Object newObj, long size) {
        synchronized(measuring) {
          if (measuring && !desc.startsWith("com/google/monitoring/runtime/instrumentation/AllocationClassAdapter")) {
            objectsAllocated++;
            bytesAllocated += size;
            if (!objectsBySize.containsKey(size))
              objectsBySize.put(size, 1L);
            else {
              long old = objectsBySize.get(size);
              objectsBySize.put(size, old + 1);
            }
            if (!bytesBySize.containsKey(size))
              bytesBySize.put(size, size);
            else {
              long old = bytesBySize.get(size);
              bytesBySize.put(size, old + size);
            }
            if (!objectsByType.containsKey(desc))
              objectsByType.put(desc, 1L);
            else {
              long old = objectsByType.get(desc);
              objectsByType.put(desc, old + 1);
            }
            if (!bytesByType.containsKey(desc))
              bytesByType.put(desc, size);
            else {
              long old = bytesByType.get(desc);
              bytesByType.put(desc, old + size);
            }
          }
        }
      }
    });
  }

  /* Immediately prior to start of the benchmark */
  @Override
  public void start(String benchmark) {
    objectsAllocated = 0;
    bytesAllocated = 0;
    objectsBySize = new TreeMap();
    bytesBySize = new TreeMap();
    objectsByType = new TreeMap();
    bytesByType = new TreeMap();
    measuring = true;
    super.start(benchmark);
  };

  /* Immediately after the end of the benchmark */
  @Override
  public void stop(long duration) {
    measuring = false;
    super.stop(duration);
    // report();
  };

  @Override
  public void complete(String benchmark, boolean valid) {
    super.complete(benchmark, valid);
    report();
  };

  private void report() {
    System.out.println("objects-allocated: "+objectsAllocated);
    System.out.println("bytes-allocated: "+bytesAllocated);
    System.out.println("objects-by-size: ");
    for (long size : objectsBySize.keySet()) {
      System.out.println("  "+size+": "+objectsBySize.get(size));
    }
    System.out.println("bytes-by-size: ");
    for (long size : objectsBySize.keySet()) {
      System.out.println("  "+size+": "+bytesBySize.get(size));
    }
    System.out.println("objects-by-type: ");
    for (String desc : objectsByType.keySet()) {
      System.out.println("  "+desc+": "+objectsByType.get(desc));
    }
    System.out.println("bytes-by-type: ");
    for (String desc : objectsByType.keySet()) {
      System.out.println("  "+desc+": "+bytesByType.get(desc));
    }
  }
}
