package org.dacapo.kafka;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Random;

public class ClientRunner{

    Method clientStarter;

    public ClientRunner() throws Exception{
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
    protected void runClient(String produceBench) throws Exception{
        Random rand = new Random();
        // Running the benchmark
        String pID = "producer" + rand.nextDouble();
        System.out.println("Trogdor is running the benchmark....");
        clientStarter.invoke(null, (Object) new String[]{"createTask", "-t", "localhost:8889", "-i", pID, "--spec", produceBench});

        // Return the information about the benchmark
        while (!System.getProperty("TaskState").equals("DONE")) {
            clientStarter.invoke(null, (Object) new String[]{"showTask", "-t", "localhost:8889", "-i", pID});
            Thread.sleep(100);
        }
        System.out.println("Finished");
    }
}
