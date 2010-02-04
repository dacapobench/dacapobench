package org.dacapo.transform;

public class DumpClass {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		System.out.print("DumpClass { ");
		if (0 < args.length) System.out.print(args[0]);
		for(int i=0; i<args.length; i++)
			System.out.print(", "+args[i]);
		System.out.println(" }");
	}

}
