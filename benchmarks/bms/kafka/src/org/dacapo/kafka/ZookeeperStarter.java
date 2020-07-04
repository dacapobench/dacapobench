package org.dacapo.kafka;

import java.lang.reflect.Method;

public class ZookeeperStarter extends Initializer{

    Thread thread;

    public ZookeeperStarter(String configZookeeper) throws Exception{
        ClassLoader loader = Thread.currentThread().getContextClassLoader();

        Class zookeeper = Class.forName("org.apache.zookeeper.server.ZooKeeperServerMain", true, loader);
        Method starterZookeeper = zookeeper.getMethod("main", String[].class);

        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    starterZookeeper.invoke(null, (Object) new String[]{configZookeeper});
                    System.out.println("...Zookeeper has completed.");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void initialize() throws Exception {
        System.out.println("Starting Zookeeper...");
        thread.start();
    }
}
