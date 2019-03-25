package org.dacapo.kafka;

import java.lang.reflect.Method;

public class ServerStarter extends Initializer{

    /**
     * @param serverKafka path to kafka configuration file
     * @throws Exception
     */
    public void initialize(String serverKafka) throws Exception{
        ClassLoader loader = Thread.currentThread().getContextClassLoader();

        // Get kafka server starter
        Class kafka = Class.forName("kafka.Kafka", true, loader);
        Method kafkaServerStarter = kafka.getMethod("main", String[].class);

        System.out.println("Starting Kafka Server...");
        kafkaServerStarter.invoke(null, (Object) new String[]{serverKafka});
        System.out.println("Shutdown Kafka Server...");

    }
}
