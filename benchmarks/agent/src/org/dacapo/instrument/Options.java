package org.dacapo.instrument;

import java.util.LinkedList;

class Options {
	LinkedList<String> options = new LinkedList<String>();
	
	private final String CLASSES_INITIALIZED = "clinit";
	
	private boolean classesInitialized = false;
	
	Options(String opts) {
		for(String opt: opts.split(",")) options.addLast(opt);

		classesInitialized = options.contains(CLASSES_INITIALIZED);
	}
	
	boolean classesInitialized() {
		return classesInitialized;
	}
	
}
