package org.dacapo.instrument;

import java.util.LinkedList;

class Options {
	LinkedList<String> options = new LinkedList<String>();
	
	private final String CLASSES_INITIALIZATION = "clinit";
	private final String METHOD_CALLS = "method_calls";
	
	private boolean classInitialization = false;
	private boolean methodCalls = false;
	
	Options(String opts) {
		for(String opt: opts.split(",")) options.addLast(opt);

		classInitialization = options.contains(CLASSES_INITIALIZATION);
		methodCalls = options.contains(METHOD_CALLS);
	}
	
	boolean classInitialization() {
		return classInitialization;
	}
	
	boolean methodCalls() {
		return methodCalls;
	}
	
	boolean runtime() {
		return false;
	}
	
}
