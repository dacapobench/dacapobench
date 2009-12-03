package org.dacapo.harness;

import java.io.File;
import java.lang.reflect.Constructor;

import org.dacapo.parser.Config;

/** Simple command-line based search demo. */
public class Lusearch extends org.dacapo.harness.Benchmark {
	private final Object benchmark;

	public Lusearch(Config config, File scratch) throws Exception {
		super(config, scratch, false);
		Class<?> clazz = Class.forName("org.dacapo.lusearch.Search", true,
				loader);
		this.method = clazz.getMethod("main", String[].class);
		Constructor<?> cons = clazz.getConstructor();
		useBenchmarkClassLoader();
		try {
			benchmark = cons.newInstance();
		} finally {
			revertClassLoader();
		}
	}

	@Override
	public void iterate(String size) throws Exception {
		method.invoke(benchmark,
				(Object) (config.preprocessArgs(size, scratch)));
	}

	/**
	 * Stub which exists <b>only</b> to facilitate whole program static analysis
	 * on a per-benchmark basis. See also the "split-deps" ant build target,
	 * which is also provided to enable whole program static analysis.
	 * 
	 * @author Eric Bodden
	 */
	public static void main(String args[]) throws Exception {
		// create dummy harness and invoke with dummy arguments
		(new Lusearch(null, null)).run(null, "");
	}
}
