package org.dacapo.kafka;


import java.io.File;
import java.net.Socket;

public class Launcher {

    private File configZookeeper;
    private File serverKafka;
    private File log4jProperties;
    private File agentConfig;
    private File produceBench;
    private File scratch;

    public Launcher(File scratch) {
        this.scratch = scratch;
    }

    public void launching() throws Exception{
        setUpScratch();
        setSystemProperty();

        invoker(new ZookeeperStarter(), configZookeeper.getPath());
        while (!hostUsed("127.0.0.1", 2181)) Thread.sleep(100);

        invoker(new ServerStarter(), serverKafka.getPath());
        while (!hostUsed("127.0.0.1", 9092)) Thread.sleep(100);
        invoker(new AgentStarter(), agentConfig.getPath());
        while (!hostUsed("127.0.0.1", 8888)) Thread.sleep(100);
        invoker(new CoordinatorStarter(), agentConfig.getPath());
        while (!hostUsed("127.0.0.1", 8889)) Thread.sleep(100);

        ClientRunner cli = new ClientRunner();
        cli.runClient(produceBench.getPath());
    }

    protected void invoker(Initializer initializer, String config){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    initializer.initialize(config);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
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
        this.produceBench = new File(scratch, "simple_consume_bench_spec.json");
    }

    private void setSystemProperty() {
        System.setProperty("log4j.configuration", "file:" + this.log4jProperties.getPath());
//        System.setProperty("com.sun.management.jmxremote");
        System.setProperty("com.sun.management.jmxremote.authenticate", "false");
        System.setProperty("com.sun.management.jmxremote.ssl", "false");
        System.setProperty("kafka.logs.dir", this.scratch.getPath());
    }
}
