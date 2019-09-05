package org.dacapo.kafka;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Random;

public class ClientRunner{

    private Method clientStarter;
    private String pID = "producer";


    ClientRunner() throws Exception{
        initialize();
    }

    public void initialize() throws Exception{
        ClassLoader loader = Thread.currentThread().getContextClassLoader();

        Class trogdorClient = Class.forName("org.apache.kafka.trogdor.coordinator.CoordinatorClient", true, loader);
        clientStarter = trogdorClient.getMethod("main", String[].class);
    }

    /**
     *
     * @param produceBench information about the benchmark
     */
    void runClient(String produceBench) throws Exception{
        // Running the benchmark
        System.out.println("Trogdor is running the benchmark....");
        clientStarter.invoke(null, (Object) new String[]{"createTask", "-t", "localhost:8889", "-i", pID, "--spec", produceBench});

        // waiting for finishing
        clientStarter.invoke(null, (Object) new String[]{"waitTask", "-t", "localhost:8889", "-i", pID});
        System.out.println("Finished");
    }

    void finishUp() throws Exception{
        clientStarter.invoke(null, (Object) new String[]{"destroyTask", "-t", "localhost:8889", "-i", pID});
    }
}
