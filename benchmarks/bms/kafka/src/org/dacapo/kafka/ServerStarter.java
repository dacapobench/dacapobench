package org.dacapo.kafka;

import java.io.FileNotFoundException;
import java.lang.reflect.Method;

public class ServerStarter extends Initializer{

    Thread thread;

    public ServerStarter(String serverKafka) throws Exception{
        ClassLoader loader = Thread.currentThread().getContextClassLoader();

        // Get kafka server starter
        Class kafka = Class.forName("kafka.Kafka", true, loader);
        Method kafkaServerStarter = kafka.getMethod("main", String[].class);

        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    kafkaServerStarter.invoke(null, (Object) new String[]{serverKafka});
                    System.out.println("Kafka Server has completed...");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void initialize() throws Exception {
        System.out.println("Starting Kafka Server...");
        thread.start();
    }
}
