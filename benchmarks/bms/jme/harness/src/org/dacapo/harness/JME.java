/*
 * Copyright (c) 2018-2020 The Australian National University.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License v2.0.
 * You may obtain the license at
 *
 *    http://www.opensource.org/licenses/apache2.0.php
 */

package org.dacapo.harness;

import org.dacapo.harness.LatencyReporter;
import org.dacapo.parser.Config;
import java.io.File;

import sun.misc.Unsafe;
import java.lang.reflect.Field;

public class JME extends Benchmark{

    private String[] args;

    public JME(Config config, File scratch, File data) throws Exception {
        super(config, scratch, data, false, false, false);
        Class<?> clazz = Class.forName("jme3test.TestChooser", true, loader);
        this.method = clazz.getMethod("main", String[].class);
    }

    @Override
    protected void prepare(String size) throws Exception {
        super.prepare(size);
        args = config.preprocessArgs(size, scratch, data);

        /*
         * FIXME
         * 
         * This workaround silences JDK11 warnings relating to use of
         * reflection.
         *
         * Specifically, jme generates the following warning:
         * 
         * WARNING: Illegal reflective access by com.jme3.util.ReflectionAllocator (file:[...]/jme/jme3-core.jar) to method sun.nio.ch.DirectBuffer.cleaner()
         *
         * Fixing the underlying issue means changing the upstream library,
         * which is beyond the scope of this benchmarking suite.
         */
        try {
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            Unsafe u = (Unsafe) theUnsafe.get(null);
      
            Class cls = Class.forName("jdk.internal.module.IllegalAccessLogger");
            Field logger = cls.getDeclaredField("logger");
            u.putObjectVolatile(cls, u.staticFieldOffset(logger), null);
        } catch (Exception e) {
            // ignore
        }
    }

    @Override
    public void iterate(String size) throws Exception {
        LatencyReporter.initialize(Integer.parseInt(args[1]), 1);

        // getting the number of frame needed to be rendered
        System.setProperty("framesToRender", args[1]);

        String[] arg = new String[] {args[0]};
        this.method.invoke(null, (Object) arg);
    }
}
