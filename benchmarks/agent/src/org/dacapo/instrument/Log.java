package org.dacapo.instrument;

public final class Log {

	public static void reportClass(String className) {
		System.out.println("C:"+className);
		return;
	}
	
	public static boolean reportMethod(String className, String methodName) {
		System.out.println("M:"+className+"."+methodName);
		return true;
	}
	
}
