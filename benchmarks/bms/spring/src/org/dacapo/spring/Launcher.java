package org.dacapo.spring;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.io.IOException;
import java.net.Socket;


public class Launcher {

    public Launcher(File scratch, File data, String[] bench) {

    }

    public static void launch(String pathToJar) throws Exception {
		// Class name to Class object mapping.
		final Map<String, Class<?>> classMap = new HashMap<>();

		final JarFile jarFile = new JarFile(pathToJar);
		final Enumeration<JarEntry> jarEntryEnum = jarFile.entries();

		final URL[] urls = { new URL("jar:file:" + pathToJar + "!/") };
		final URLClassLoader urlClassLoader = URLClassLoader.newInstance(urls);

        while (jarEntryEnum.hasMoreElements()) {

            final JarEntry jarEntry = jarEntryEnum.nextElement();
            final String jarEntryName = jarEntry.getName();
            
            if (jarEntryName.startsWith("org/springframework/boot")
            && jarEntryName.endsWith(".class") == true) {
            
                int endIndex = jarEntryName.lastIndexOf(".class");
                
                String className = jarEntryName.substring(0, endIndex).replace('/', '.');
                
                try {
                    final Class<?> loadedClass = urlClassLoader.loadClass(className);
                    classMap.put(loadedClass.getName(), loadedClass);
                }
                catch (final ClassNotFoundException ex) {
                
                }
            }
        }
        jarFile.close();


        // Create JarFileArchive(File) object, needed for JarLauncher.
        final Class<?> jarFileArchiveClass = classMap.get("org.springframework.boot.loader.archive.JarFileArchive");
        final Constructor<?> jarFileArchiveConstructor = jarFileArchiveClass.getConstructor(File.class);
        final Object jarFileArchive = jarFileArchiveConstructor.newInstance(new File(pathToJar));

        final Class<?> archiveClass = classMap.get("org.springframework.boot.loader.archive.Archive");
				
        final Class mainClass = classMap.get("org.springframework.boot.loader.JarLauncher");

        // Create JarLauncher object using JarLauncher(Archive) constructor. 
        final Constructor<?> jarLauncherConstructor = mainClass.getDeclaredConstructor(archiveClass);

        jarLauncherConstructor.setAccessible(true);
        final Object jarLauncher = jarLauncherConstructor.newInstance(jarFileArchive);

        // Invoke JarLauncher#launch(String[]) method.
        final Class<?> launcherClass = 	classMap.get("org.springframework.boot.loader.Launcher");

        final Method launchMethod = launcherClass.getDeclaredMethod("launch", String[].class);
        launchMethod.setAccessible(true);
		launchMethod.invoke(jarLauncher, new Object[]{new String[0]});
    }

    public void launching() throws Exception{
        // setUpData();
        // setSystemProperty();

        // ZookeeperStarter zoo = new ZookeeperStarter(configZookeeper.getPath());
        // zoo.initialize();
        // while (!hostUsed("127.0.0.1", 2181)) Thread.sleep(100);

        // ServerStarter kafka = new ServerStarter(serverKafka.getPath());
        // kafka.initialize();
        // while (!hostUsed("127.0.0.1", 9092)) Thread.sleep(100);

        // AgentStarter agent = new AgentStarter(agentConfig.getPath());
        // agent.initialize();
        // while (!hostUsed("127.0.0.1", 8888)) Thread.sleep(100);

        // CoordinatorStarter cs = new CoordinatorStarter(agentConfig.getPath());
        // cs.initialize();
        // while (!hostUsed("127.0.0.1", 8889)) Thread.sleep(100);
    }

    public void performIteration() throws Exception {
        // cli = new ClientRunner(transactions);
        // cli.runClient(produceBench.getPath());
    }

    public void shutdown() throws Exception {
        // cli.finishUp();
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

    private void setUpData() {
        // this.configZookeeper = new File(data, "zookeeper.properties");
        // this.serverKafka = new File(data, "server.properties");
        // this.log4jProperties = new File(data, "tools-log4j.properties");
        // this.agentConfig = new File(data, "trogdor.conf");
    }

    private void setSystemProperty() {
//         System.setProperty("log4j.configuration", "file:" + this.log4jProperties.getPath());
// //        System.setProperty("com.sun.management.jmxremote");
//         System.setProperty("com.sun.management.jmxremote.authenticate", "false");
//         System.setProperty("com.sun.management.jmxremote.ssl", "false");
//         System.setProperty("kafka.logs.dir", this.scratch.getPath());
//         System.setProperty("TaskState", "TODO");
    }

}
