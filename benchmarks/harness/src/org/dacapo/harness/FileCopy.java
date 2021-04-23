/*
 * Copyright (c) 2005, 2009 The Australian National University.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License v2.0.
 * You may obtain the license at
 * 
 *    http://www.opensource.org/licenses/apache2.0.php
 */
package org.dacapo.harness;

import java.io.*;
import java.nio.file.attribute.*;
import java.nio.file.*;

public class FileCopy extends SimpleFileVisitor<Path> {
    private Path src;
    private Path tgt;
 
    public FileCopy(Path src, Path tgt) {
        this.src = src;
        this.tgt = tgt;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attr) {
      try {
        Path tgtFile = tgt.resolve(src.relativize(file));
        Files.copy(file, tgtFile);
      } catch (IOException ex) {
        System.err.println(ex);
      }
      return FileVisitResult.CONTINUE;
    }
 
    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attr) {
      try {
        Path newDir = tgt.resolve(src.relativize(dir));
        Files.createDirectories(newDir);
      } catch (IOException ex) {
        System.err.println(ex);
      }
      return FileVisitResult.CONTINUE;
    }
}