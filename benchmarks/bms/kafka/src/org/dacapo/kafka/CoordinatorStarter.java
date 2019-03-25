package org.dacapo.kafka;

import java.lang.reflect.Method;

public class CoordinatorStarter extends Initializer{

    public void initialize(String coordinatorConfig) throws Exception{
        ClassLoader loader = Thread.currentThread().getContextClassLoader();

        Class trogdorCoordinator = Class.forName("org.apache.kafka.trogdor.coordinator.Coordinator", true, loader);
        Method coordinatorStarter = trogdorCoordinator.getMethod("main", String[].class);

        System.out.println("Starting Coordinator...");
        coordinatorStarter.invoke(null, (Object) new String[]{"-c", coordinatorConfig, "-n", "node0"});
        System.out.println("Shutdown Coordinator...");
    }
}
