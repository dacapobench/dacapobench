package org.dacapo.kafka;


import java.io.File;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class Launcher {

    private File configZookeeper;
    private File serverKafka;
    private File log4jProperties;
    private File agentConfig;
    private File produceBench;
    private File scratch;
    private ClientRunner cli;

    public Launcher(File scratch, String[] bench) {
        this.scratch = scratch;
        this.produceBench = new File(scratch, bench[0]);
    }

    public void launching() throws Exception{
        setUpScratch();
        setSystemProperty();

        ZookeeperStarter zoo = new ZookeeperStarter(configZookeeper.getPath());
        zoo.initialize();
        while (!hostUsed("127.0.0.1", 2181)) Thread.sleep(100);

        ServerStarter kafka = new ServerStarter(serverKafka.getPath());
        kafka.initialize();
        while (!hostUsed("127.0.0.1", 9092)) Thread.sleep(100);

        AgentStarter agent = new AgentStarter(agentConfig.getPath());
        agent.initialize();
        while (!hostUsed("127.0.0.1", 8888)) Thread.sleep(100);

        CoordinatorStarter cs = new CoordinatorStarter(agentConfig.getPath(), scratch);
        cs.initialize();
        while (!hostUsed("127.0.0.1", 8889)) Thread.sleep(100);
    }

    public void performIteration() throws Exception {
        cli = new ClientRunner();
        cli.runClient(produceBench.getPath());
    }

    public void shutdown() throws Exception {
        cli.finishUp();
    }

    private boolean hostUsed(String host, int port){
        boolean used = false;
        try {
            Socket socket = new Socket(host, port);
            used = true;
        } catch (Exception ignored) {
        }
        return used;
    }

    private void setUpScratch() {
        this.configZookeeper = new File(scratch, "zookeeper.properties");
        this.serverKafka = new File(scratch, "server.properties");
        this.log4jProperties = new File(scratch, "tools-log4j.properties");
        this.agentConfig = new File(scratch, "trogdor.conf");
    }

    private void setSystemProperty() {
        System.setProperty("log4j.configuration", "file:" + this.log4jProperties.getPath());
//        System.setProperty("com.sun.management.jmxremote");
        System.setProperty("com.sun.management.jmxremote.authenticate", "false");
        System.setProperty("com.sun.management.jmxremote.ssl", "false");
        System.setProperty("kafka.logs.dir", this.scratch.getPath());
        System.setProperty("TaskState", "TODO");
    }
}
