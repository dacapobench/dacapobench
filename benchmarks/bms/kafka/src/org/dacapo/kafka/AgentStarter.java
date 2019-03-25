package org.dacapo.kafka;

import java.lang.reflect.Method;

public class AgentStarter extends Initializer{

    /**
     * @param agentConfig path to agent configuration file
     */
    public void initialize(String agentConfig) throws Exception{
        ClassLoader loader = Thread.currentThread().getContextClassLoader();

        Class trogdorAgent = Class.forName("org.apache.kafka.trogdor.agent.Agent", true, loader);
        Method agentStarter = trogdorAgent.getMethod("main", String[].class);

        System.out.println("Starting Agent...");
        agentStarter.invoke(null, (Object) new String[]{"-c", agentConfig, "-n", "node0"});
        System.out.println("Shutdown Agent...");
    }

}
