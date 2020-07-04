package org.dacapo.kafka;

import org.dacapo.harness.Benchmark;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;

public class AgentStarter extends Initializer{

    Thread thread;

    public AgentStarter(String agentConfig) throws Exception{
        ClassLoader loader = Thread.currentThread().getContextClassLoader();

        Class trogdorAgent = Class.forName("org.apache.kafka.trogdor.agent.Agent", true, loader);
        Method agentStarter = trogdorAgent.getMethod("main", String[].class);

        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    agentStarter.invoke(null, (Object) new String[]{"-c", agentConfig, "-n", "node0"});
                    System.out.println("...Agent has completed.");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void initialize() throws Exception {
        System.out.println("Starting Agent...");
        thread.start();
    }
}
