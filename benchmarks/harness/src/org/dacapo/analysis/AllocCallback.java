/**
 * Example usage:
 *
 * java -javaagent:<dacapo_version>/jar/java-allocation-instrumenter-3.3.4.jar -Ddacapo.alloc.yml=<output_file> -jar <dacapo_version>.jar -callback org.dacapo.analysis.AllocCallback <benchmark>
 */
package org.dacapo.analysis;

import java.util.Map;
import java.util.TreeMap;
import java.io.File;
import java.io.PrintStream;
import org.dacapo.harness.Callback;
import org.dacapo.harness.CommandLineArgs;
import com.google.monitoring.runtime.instrumentation.AllocationRecorder;
import com.google.monitoring.runtime.instrumentation.Sampler;

/*
 * This code depends on the java-allocation-instrumenter library,
 * which is included in the dacapo artifact as part of the build
 * process.
 *
 * See: https://github.com/google/allocation-instrumenter/
 */
public class AllocCallback extends Callback {
  private static final String YML_FILENAME_PROPERTY = "dacapo.alloc.yml";

  private static int iteration = 0;
  private static String ymlSuffix = "";

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
    ymlSuffix = isWarmup() ? "."+iteration : "";
    // report();
  };

  @Override
  public void complete(String benchmark, boolean valid) {
    super.complete(benchmark, valid);
    report();
  };

  private void report() {
    PrintStream yml = System.out;

    String ymlfile = System.getProperty(YML_FILENAME_PROPERTY);
    if (ymlfile == null) {
      System.out.println("The '"+YML_FILENAME_PROPERTY+"' system property is not set, so printing alloc yml to console.");
    } else {
      ymlfile += ymlSuffix;
      try {
          yml = new PrintStream(new File(ymlfile));
      } catch (Exception e) {
          System.err.println("Could not open '"+ymlfile+"', so printing bytecode yml to console.");
      }
    }

    yml.println("# These statistics can be generated from a dacapo release using a command line like:");
    yml.println("#    java -javaagent:<dacapo_version>/jar/java-allocation-instrumenter-3.3.4.jar -Ddacapo.alloc.yml=<output_file> -jar <dacapo_version>.jar -callback org.dacapo.analysis.AllocCallback <benchmark>");
    yml.println("#");

    yml.println("objects-allocated: "+objectsAllocated);
    yml.println("bytes-allocated: "+bytesAllocated);
    yml.println("# number of objects allocated for each object size");
    yml.println("objects-by-size: ");
    for (long size : objectsBySize.keySet()) {
      yml.println("  "+size+": "+objectsBySize.get(size));
    }
    yml.println("# number of bytes allocated for each object size");
    yml.println("bytes-by-size: ");
    for (long size : objectsBySize.keySet()) {
      yml.println("  "+size+": "+bytesBySize.get(size));
    }
    yml.println("# number of objects allocated for each class");
    yml.println("objects-by-type: ");
    for (String desc : objectsByType.keySet()) {
      yml.println("  "+desc.replaceAll("\\\\","")+": "+objectsByType.get(desc));
    }
    yml.println("# number of bytes allocated for each class");
    yml.println("bytes-by-type: ");
    for (String desc : objectsByType.keySet()) {
      yml.println("  "+desc.replaceAll("\\\\","")+": "+bytesByType.get(desc));
    }
  }
}
