package org.dacapo.kafka;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Random;
import java.io.*;

public class ClientRunner{

    private Method agentStarter;
    private Method setTxCount;
    private Method topicCommand;
    long timerBase = 0;
    private int txCount;
    private final static long timeout = (long) (1000L * Float.parseFloat(System.getProperty("dacapo.timeout.dialation")));

    ClientRunner(int txCount) throws Exception{
        this.txCount = txCount;
        initialize();
    }

    public void initialize() throws Exception{
        ClassLoader loader = Thread.currentThread().getContextClassLoader();

        Class worker = Class.forName("org.apache.kafka.trogdor.workload.ProduceBenchWorker");
        setTxCount = worker.getMethod("setTxCount", int.class);

        Class trogdorAgent = Class.forName("org.apache.kafka.trogdor.agent.Agent", true, loader);
        agentStarter = trogdorAgent.getMethod("main", String[].class);

        Class topicClass = Class.forName("kafka.admin.TopicCommand", true, loader);
        topicCommand = topicClass.getMethod("main", String[].class);
    }

    /**
     *
     * @param produceBench information about the benchmark
     */
    void runClient(String agentConfig, String produceBench) throws Exception{
        // Running the benchmark  
        System.out.println("Trogdor is running the workload....");
        timerBase = System.nanoTime();
        setTxCount.invoke(null, txCount);
        // Using the Trogodr exec mode to send requests
        agentStarter.invoke(null, (Object) new String[]{"-c", agentConfig, "-n", "node0", "--exec", produceBench});
        topicCommand.invoke(null, (Object) new String[]{"--bootstrap-server", "localhost:9092", "--delete", "--topic", "dacapo-1,dacapo-2,dacapo-3,dacapo-4"});

    }

    void finishUp() throws Exception{
        // Sleep one second waiting for the Kafka broker to delete the topics
        System.err.println("Waiting " + timeout + "ms for cleanup...");
        Thread.sleep(timeout);
        System.err.println("Finished");
    }
}
