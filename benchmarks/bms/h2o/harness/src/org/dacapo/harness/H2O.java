/*
 * Copyright (c) 2018 The Australian National University.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License v2.0.
 * You may obtain the license at
 *
 *    http://www.opensource.org/licenses/apache2.0.php
 */

package org.dacapo.harness;

import org.dacapo.h2o.ClientRunner;
import org.dacapo.parser.Config;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

/**
 * Dacapo benchmark harness for H2O
 *
 * date:  $Date: 2018-2-10 $
 */

public class H2O extends Benchmark{

    private String[] args;
    PrintStream savedOut;
    private String ip = "127.0.0.1";
    private String port = "54321";

    public H2O(Config config, File scratch, File data) throws Exception {
        super(config, scratch, data, false);
        Class<?> clazz = Class.forName("water.H2OApp", true, loader);
        this.method = clazz.getMethod("main", String[].class);
    }

    @Override
    protected void prepare(String size) throws Exception {
        args = config.preprocessArgs(size, scratch, data);
        emptyOutput();

        // Launch the h2o server
        useBenchmarkClassLoader();
        this.method.invoke(null,  (Object) new String[] {"-ip", ip, "-port", port});
    }

    @Override
    public void iterate(String size) throws Exception {

        PrintStream savedOut = System.out;
        PrintStream savedErr= System.err;
        // Store the standard output
        emptyOutput();

        ClientRunner.running("http://" + ip + ":" + port, args[0], args[1], args[2], args[3], savedOut, savedErr);

        System.setOut(savedOut);
    }

    private void emptyOutput(){
        // Store the standard output
        System.setOut(new PrintStream(new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                // Doing nothing
            }
        }));
    }
}
