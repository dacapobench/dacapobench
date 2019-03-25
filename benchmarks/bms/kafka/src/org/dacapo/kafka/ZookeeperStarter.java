package org.dacapo.kafka;

import java.lang.reflect.Method;

public class ZookeeperStarter extends Initializer{

    /**
     * @param configZookeeper Path to the configuration file of zookeeper
     * @throws Exception
     */
    public void initialize(String configZookeeper) throws Exception{
        ClassLoader loader = Thread.currentThread().getContextClassLoader();

        Class zookeeper = Class.forName("org.apache.zookeeper.server.ZooKeeperServerMain", true, loader);
        Method starterZookeeper = zookeeper.getMethod("main", String[].class);

        System.out.println("Starting Zookeeper...");
        starterZookeeper.invoke(null, (Object) new String[]{configZookeeper});
        System.out.println("Shutdown Zookeeper...");

    }

//    protected static void shutdown() throws Exception{
//        shutdownZookeeper.invoke(null);
//    }

}
