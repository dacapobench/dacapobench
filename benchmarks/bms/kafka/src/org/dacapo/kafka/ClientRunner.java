package org.dacapo.kafka;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Random;
import java.io.*;

public class ClientRunner{

    private Method clientStarter;
    private String pID = "producer";
    private Method setTxCount;
    long timerBase = 0;
    private int txCount;

    ClientRunner(int txCount) throws Exception{
        this.txCount = txCount;
        initialize();
    }

    public void initialize() throws Exception{
        ClassLoader loader = Thread.currentThread().getContextClassLoader();

        Class trogdorClient = Class.forName("org.apache.kafka.trogdor.coordinator.CoordinatorClient", true, loader);
        clientStarter = trogdorClient.getMethod("main", String[].class);

        Class worker = Class.forName("org.apache.kafka.trogdor.workload.ProduceBenchWorker");
        setTxCount = worker.getMethod("setTxCount", int.class);
    }

    /**
     *
     * @param produceBench information about the benchmark
     */
    void runClient(String produceBench) throws Exception{
        // Running the benchmark
        System.out.println("Trogdor is running the workload....");
        timerBase = System.nanoTime();
        setTxCount.invoke(null, txCount);
        clientStarter.invoke(null, (Object) new String[]{"createTask", "-t", "localhost:8889", "-i", pID, "--spec", produceBench});

        // waiting for finishing
        clientStarter.invoke(null, (Object) new String[]{"waitTask", "-t", "localhost:8889", "-i", pID});
        System.err.println("Finished");
    }

    void finishUp() throws Exception{
        clientStarter.invoke(null, (Object) new String[]{"destroyTask", "-t", "localhost:8889", "-i", pID});
    }
}
