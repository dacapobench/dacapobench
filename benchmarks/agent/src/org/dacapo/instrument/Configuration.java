package org.dacapo.instrument;

public final class Configuration {

	public static int availableProcessors(Runtime runtime) {
		return runtime.availableProcessors();
	}
	
	public static void Report() {
		System.out.println("Configuration.Report()");
	}
}
