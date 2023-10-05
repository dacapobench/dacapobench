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

import org.dacapo.harness.util.AvailablePortFinder;

public class Launcher {

  private static final int DEFAULT_SPRING_PORT = 8080;
  static final int SPRING_PORT = establishSpringPort();

  static {
    // Force character encoding to avoid a locale setting affecting benchmark behavior
    System.setProperty("spring.sql.init.encoding", "UTF-8");
  }

  public Launcher(File scratch, File data, String[] bench) {}

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
        } catch (final ClassNotFoundException e) {
          System.err.println("Problem launching server for classname '"+className+"': "+e);
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

    System.out.println("Launching the server");
    launchMethod.invoke(jarLauncher, new Object[]{new String[0]});
  }

  static int establishSpringPort() {
    // determine whether the http port was explicitly requested via command line property
    int port = DEFAULT_SPRING_PORT;
    String sp = System.getProperty("server.port");
    if (sp != null) {
      try {
        port = Integer.parseInt(sp);
      } catch (NumberFormatException e) {
        System.err.println("server.port set to malformed value '"+sp+"', exiting.");
        System.exit(-1);
      }
    }

    // adjust the ports as necessary
    int requested = port;
    if (!AvailablePortFinder.available(requested)) {
      port = AvailablePortFinder.getNextAvailable(requested);
      System.out.print("Port conflict detected. ");
    }

    System.setProperty("server.port", Integer.toString(port));
    System.out.println("Spring is using port "+port+". Configure with -Dserver.port.");
    return port;
  }
}
