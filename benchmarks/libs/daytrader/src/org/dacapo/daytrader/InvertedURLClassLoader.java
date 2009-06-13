package org.dacapo.daytrader;

import java.net.URL;

public class InvertedURLClassLoader extends java.net.URLClassLoader {
  InvertedURLClassLoader(URL[] urls, ClassLoader parent) {
    super(urls, parent);
  }
  
  public URL findResource(String name) {
    URL rtn = super.findResource(name);
    return rtn;
  }
  
  /**
   * Reverse the logic of the default classloader, by trying the top-level
   * classes first.  This way, libraries packaged with the benchmarks
   * override those provided by the runtime environment.
   */
  @Override
  protected synchronized Class<?> loadClass(String name, boolean resolve)
  throws ClassNotFoundException {
    // First, check whether the class has already been loaded
    Class<?> c = findLoadedClass(name);
    if (c == null) {
      try {
        // Next, try to resolve it from the dacapo JAR files
        c = super.findClass(name);
        if (resolve) {
          resolveClass(c);
        }
      } catch (ClassNotFoundException e) {
        // And if all else fails delegate to the parent.
        c = super.loadClass(name,resolve);
      }
    }
    return c;
  }

}
