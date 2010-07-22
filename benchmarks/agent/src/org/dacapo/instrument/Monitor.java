package org.dacapo.instrument;

public final class Monitor {
	public static void reportClassInitialization(String className) {
		System.err.println(className+".<clinit>()");
	}

}
