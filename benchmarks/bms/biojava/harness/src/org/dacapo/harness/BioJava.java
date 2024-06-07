package org.dacapo.harness;

import org.dacapo.parser.Config;

import java.io.File;
import java.io.PrintStream;

public class BioJava extends Benchmark {

    String[] args;

    public BioJava(Config config, File scratch, File data) throws Exception {
        super(config, scratch, data, false);
        assertJavaVersionGE(11, "Biojava requires Java version 11 or higher.");
        Class<?> clazz = Class.forName("org.biojava.nbio.ronn.ORonn", true, loader);
        this.method = clazz.getMethod("main", String[].class);
    }

    @Override
    protected void prepare(String size) throws Exception {
        super.prepare(size);
        args = config.preprocessArgs(size, scratch, data);
    }

    @Override
    public void iterate(String size) throws Exception {
        String in = args[0]+"="+args[1];
        String out = args[2]+"="+args[3];
        String stats = args[4]+"="+args[5];
        String threads = args[6]+"="+args[7];
        String[] newArgs = { in, out, stats, threads};
        this.method.invoke(null, (Object) newArgs);
    }
}
