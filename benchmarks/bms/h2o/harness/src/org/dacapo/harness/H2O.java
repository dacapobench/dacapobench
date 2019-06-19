/*
 * Copyright (c) 2018 The Australian National University.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License v2.0.
 * You may obtain the license at
 *
 *    http://www.opensource.org/licenses/apache2.0.php
 */

package org.dacapo.harness;

import org.dacapo.parser.Config;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

/**
 * Dacapo benchmark harness for H2O
 *
 * date:  $Date: 2018-2-10 $
 */

public class H2O extends Benchmark{

    private String[] args;

    public H2O(Config config, File scratch, File data) throws Exception {
        super(config, scratch, data, false);
        Class<?> clazz = Class.forName("water.H2OApp", true, loader);
        this.method = clazz.getMethod("main", String[].class);
    }

    @Override
    protected void prepare(String size) throws Exception {
        super.prepare(size);
        args = config.preprocessArgs(size, scratch, data);
    }

    @Override
    public void iterate(String size) throws Exception {
        this.method.invoke(null,  (Object) new String[] {"-ip", "127.0.0.1", "-port", "54321"});
        System.out.println("Testing");
        Thread.sleep(20000);
    }
}
