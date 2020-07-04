package org.dacapo.kafka;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Random;
import java.io.*;

public class ClientRunner{

    private Method clientStarter;
    private String pID = "producer";
    private Method setLatencyBuf;
    private static final int MAX_TX = 1000000;
    long[][] txTimes = new long[2][MAX_TX];
    long timerBase = 0;

    ClientRunner() throws Exception{
        initialize();
    }

    public void initialize() throws Exception{

        ClassLoader loader = Thread.currentThread().getContextClassLoader();

        Class trogdorClient = Class.forName("org.apache.kafka.trogdor.coordinator.CoordinatorClient", true, loader);
        clientStarter = trogdorClient.getMethod("main", String[].class);

        Class worker = Class.forName("org.apache.kafka.trogdor.workload.ProduceBenchWorker");
        setLatencyBuf = worker.getMethod("setLatencyBuffer", Object.class);
    }

    /**
     *
     * @param produceBench information about the benchmark
     */
    void runClient(String produceBench) throws Exception{
        // Running the benchmark
        System.out.println("Trogdor is running the workload....");
        timerBase = System.nanoTime();
        setLatencyBuf.invoke(null, (Object) txTimes);
        clientStarter.invoke(null, (Object) new String[]{"createTask", "-t", "localhost:8889", "-i", pID, "--spec", produceBench});

        // waiting for finishing
        clientStarter.invoke(null, (Object) new String[]{"waitTask", "-t", "localhost:8889", "-i", pID});
        System.err.println("Finished");
    }

    void finishUp() throws Exception{
        FileWriter dacapocsv = null;
        try {
            dacapocsv = new FileWriter(System.getProperty("dacapo.latency.csv"));
            dacapocsv.write("# (unused), start nsec, end nsec"+System.lineSeparator());
          } catch (Exception e) {
            System.out.println("Failed trying to create latency stats: "+e);
            System.exit(-1);
          }

        try {
              for (int i = 0; i < MAX_TX; i++) {
                long end = txTimes[1][i] - timerBase;
                long start = txTimes[0][i] - timerBase;
                if (txTimes[0][i] != 0) {
                  String str;
                  str = ", "+Long.toString(start)+", "+Long.toString(end)+System.lineSeparator();
                  dacapocsv.write(str);
                  txTimes[0][i] = txTimes[0][i] = 0;
                } 
              }
            dacapocsv.close();
        } catch (Exception e) {
            System.out.println("Failed trying to write latency stats: "+e);
        }

        clientStarter.invoke(null, (Object) new String[]{"destroyTask", "-t", "localhost:8889", "-i", pID});
    }
}
