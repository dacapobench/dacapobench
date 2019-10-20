package org.dacapo.kafka;

import org.dacapo.harness.Benchmark;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class CoordinatorStarter extends Initializer{

    Thread thread;

    public CoordinatorStarter(String coordinatorConfig, File scratch) throws Exception {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();

        Class trogdorCoordinator = Class.forName("org.apache.kafka.trogdor.coordinator.Coordinator", true, loader);
        Method coordinatorStarter = trogdorCoordinator.getMethod("main", String[].class);

        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    coordinatorStarter.invoke(null, (Object) new String[]{"-c", coordinatorConfig, "-n", "node0"});
                    System.out.println("Shutdown Coordinator...");

                    // no need for unnecessary messages. Benchmarking is already finished
                    System.setOut(null);
                    System.setErr(null);
                    Benchmark.deleteTree(new File(scratch, "zookeeper"));
                    Benchmark.deleteTree(new File(scratch, "kafka-logs"));
                } catch (IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void initialize() throws Exception {
        System.out.println("Starting Coordinator...");
        thread.start();
    }
}
