package org.dacapo.instrument;

public final class Configuration {

	private final static String PROCESSOR_COUNT_PROP = "dacapo.processor.count";
	
	public final static int processorCount;
	
	static {
		int pc = 0;
		try {
			pc = Math.max(0,Integer.parseInt(System.getProperty(PROCESSOR_COUNT_PROP)));
		} catch (Exception e) { }
		processorCount = pc;
	}
	
	public static int availableProcessors(Runtime runtime) {
		return (processorCount!=0)?processorCount:runtime.availableProcessors();
	}
}
