package org.dacapo.kafka;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Random;
import java.io.*;

public class ClientRunner{

    private Method clientStarter;
    private String pID = "producer";
    private Method setLatencyBuf;
    long timerBase = 0;
    long[][] txTimes = null;

    ClientRunner(long[][] txTimes) throws Exception{
        this.txTimes = txTimes;
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
          System.out.print("Writing latency data to "+System.getProperty("dacapo.latency.csv")+"...");
        try {
              for (int i = 0; i < txTimes[1].length; i++) {
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
        System.out.println("...done");

        clientStarter.invoke(null, (Object) new String[]{"destroyTask", "-t", "localhost:8889", "-i", pID});
    }
}
