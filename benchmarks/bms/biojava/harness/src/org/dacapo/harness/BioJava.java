package org.dacapo.harness;

import org.dacapo.parser.Config;

import java.io.File;
import java.io.PrintStream;

public class BioJava extends Benchmark {

    String[] args;

    public BioJava(Config config, File scratch, File data) throws Exception {
        super(config, scratch, data);
        Class<?> clazz = Class.forName("org.biojava.nbio.aaproperties.CommandPrompt", true, loader);
        this.method = clazz.getMethod("main", String[].class);
    }

    @Override
    protected void prepare(String size) throws Exception {
        super.prepare(size);
        args = config.preprocessArgs(size, scratch, data);
    }

    @Override
    public void iterate(String size) throws Exception {
        this.method.invoke(null, (Object) new String[]{"-i", args[0], "-a"});
    }
}
