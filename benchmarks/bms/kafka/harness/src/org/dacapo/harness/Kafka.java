/*
 * Copyright (c) 2019 The Australian National University.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License v2.0.
 * You may obtain the license at
 *
 *    http://www.opensource.org/licenses/apache2.0.php
 */
package org.dacapo.harness;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import org.dacapo.parser.Config;

/**
 * date:  $Date: 2009-12-24 11:19:36 +1100 (Thu, 24 Dec 2009) $
 * id: $Id: Jython.java 738 2009-12-24 00:19:36Z steveb-oss $
 */
public class Kafka extends Benchmark {
    Constructor lc;
    Method launching;

    public Kafka(Config config, File scratch, File data) throws Exception {
        super(config, scratch, data, false);
        Class launcher = Class.forName("org.dacapo.kafka.Launcher", true, this.loader);
        lc = launcher.getConstructor(File.class);
        launching = launcher.getMethod("launching");
    }

    @Override
    public void iterate(String size) throws Exception {
        launching.invoke(lc.newInstance(this.scratch));
    }

}
