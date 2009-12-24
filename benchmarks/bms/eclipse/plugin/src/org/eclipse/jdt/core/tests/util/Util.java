/*
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Nina Rinskaya
 *              Fix for https://bugs.eclipse.org/bugs/show_bug.cgi?id=172820.
 */
package org.eclipse.jdt.core.tests.util;

import java.io.*;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.zip.*;

public class Util {

  /**
   * Search the user hard-drive for a Java class library. Returns null if none
   * could be found.
   * 
   * Example of use: [org.eclipse.jdt.core.tests.util.Util.getJavaClassLib()]
   * 
   * @date $Date: 2009-12-24 11:19:36 +1100 (Thu, 24 Dec 2009) $
   * @id $Id: Util.java 738 2009-12-24 00:19:36Z steveb-oss $
   */
  public static String[] getJavaClassLibs() {
    // check bootclasspath properties for Sun, JRockit and Harmony VMs
    String bootclasspathProperty = System.getProperty("sun.boot.class.path"); //$NON-NLS-1$
    if ((bootclasspathProperty == null) || (bootclasspathProperty.length() == 0)) {
      // IBM J9 VMs
      bootclasspathProperty = System.getProperty("vm.boot.class.path"); //$NON-NLS-1$
      if ((bootclasspathProperty == null) || (bootclasspathProperty.length() == 0)) {
        // Harmony using IBM VME
        bootclasspathProperty = System.getProperty("org.apache.harmony.boot.class.path"); //$NON-NLS-1$
      }
    }
    String[] jars = null;
    if ((bootclasspathProperty != null) && (bootclasspathProperty.length() != 0)) {
      StringTokenizer tokenizer = new StringTokenizer(bootclasspathProperty, File.pathSeparator);
      final int size = tokenizer.countTokens();
      jars = new String[size];
      int i = 0;
      while (tokenizer.hasMoreTokens()) {
        final String fileName = toNativePath(tokenizer.nextToken());
        if (new File(fileName).exists()) {
          jars[i] = fileName;
          i++;
        }
      }
      if (size != i) {
        // resize
        System.arraycopy(jars, 0, (jars = new String[i]), 0, i);
      }
    } else {
      String jreDir = getJREDirectory();
      final String osName = System.getProperty("os.name");
      if (jreDir == null) {
        return new String[] {};
      }
      if (osName.startsWith("Mac")) {
        return new String[] { toNativePath(jreDir + "/../Classes/classes.jar") };
      }
      final String vmName = System.getProperty("java.vm.name");
      if ("J9".equals(vmName)) {
        return new String[] { toNativePath(jreDir + "/lib/jclMax/classes.zip") };
      }
      String[] jarsNames = null;
      ArrayList paths = new ArrayList();
      if ("DRLVM".equals(vmName)) {
        FilenameFilter jarFilter = new FilenameFilter() {
          public boolean accept(File dir, String name) {
            return name.endsWith(".jar") & !name.endsWith("-src.jar");
          }
        };
        jarsNames = new File(jreDir + "/lib/boot/").list(jarFilter);
        addJarEntries(jreDir + "/lib/boot/", jarsNames, paths);
      } else {
        jarsNames = new String[] { "/lib/vm.jar", "/lib/rt.jar", "/lib/core.jar", "/lib/security.jar", "/lib/xml.jar", "/lib/graphics.jar" };
        addJarEntries(jreDir, jarsNames, paths);
      }
      jars = new String[paths.size()];
      paths.toArray(jars);
    }
    return jars;
  }

  private static void addJarEntries(String jreDir, String[] jarNames, ArrayList paths) {
    for (int i = 0, max = jarNames.length; i < max; i++) {
      final String currentName = jreDir + jarNames[i];
      File f = new File(currentName);
      if (f.exists()) {
        paths.add(toNativePath(currentName));
      }
    }
  }

  /**
   * Returns the JRE directory this tests are running on. Returns null if none
   * could be found.
   * 
   * Example of use: [org.eclipse.jdt.core.tests.util.Util.getJREDirectory()]
   */
  public static String getJREDirectory() {
    return System.getProperty("java.home");
  }

  /**
   * Makes the given path a path using native path separators as returned by
   * File.getPath() and trimming any extra slash.
   */
  public static String toNativePath(String path) {
    String nativePath = path.replace('\\', File.separatorChar).replace('/', File.separatorChar);
    return nativePath.endsWith("/") || nativePath.endsWith("\\") ? nativePath.substring(0, nativePath.length() - 1) : nativePath;
  }

  /**
   * Unzip the contents of the given zip in the given directory (create it if it
   * doesn't exist)
   */
  public static void unzip(String zipPath, String destDirPath) throws IOException {

    InputStream zipIn = new FileInputStream(zipPath);
    byte[] buf = new byte[8192];
    File destDir = new File(destDirPath);
    ZipInputStream zis = new ZipInputStream(zipIn);
    FileOutputStream fos = null;
    try {
      ZipEntry zEntry;
      while ((zEntry = zis.getNextEntry()) != null) {
        // if it is empty directory, create it
        if (zEntry.isDirectory()) {
          new File(destDir, zEntry.getName()).mkdirs();
          continue;
        }
        // if it is a file, extract it
        String filePath = zEntry.getName();
        int lastSeparator = filePath.lastIndexOf("/"); //$NON-NLS-1$
        String fileDir = ""; //$NON-NLS-1$
        if (lastSeparator >= 0) {
          fileDir = filePath.substring(0, lastSeparator);
        }
        // create directory for a file
        new File(destDir, fileDir).mkdirs();
        // write file
        File outFile = new File(destDir, filePath);
        fos = new FileOutputStream(outFile);
        int n = 0;
        while ((n = zis.read(buf)) >= 0) {
          fos.write(buf, 0, n);
        }
        fos.close();
      }
    } catch (IOException ioe) {
      if (fos != null) {
        try {
          fos.close();
        } catch (IOException ioe2) {
        }
      }
    } finally {
      try {
        zipIn.close();
        if (zis != null)
          zis.close();
      } catch (IOException ioe) {
      }
    }
  }

}
