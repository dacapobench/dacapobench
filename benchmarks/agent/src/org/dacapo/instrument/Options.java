package org.dacapo.instrument;

import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

class Options {
	private TreeMap<String,String> options = new TreeMap<String,String>();
	private Set<String>            keys;
	
	public static final String CLASSES_INITIALIZATION = "clinit";
	public static final String METHOD_INSTR           = "method_instr";
	public static final String LOG_START              = "log_start";
	public static final String LOG_STOP               = "log_stop";
	public static final String RUNTIME                = "runtime";
	public static final String ALLOCATE               = "allocate";
	public static final String POINTER                = "pointer";
	public static final String MONITOR                = "monitor";
	public static final String CALL_CHAIN             = "call_chain";
	public static final String EXCLUDE_PACKAGES       = "exclude";
	
	private boolean classInitialization = false;
	private boolean methodCalls = false;
	
	public static void main(String[] args) {
		String r = "";
		
		if (0<args.length) {
			r=args[0];
			for(int i=1; i<args.length; i++) r+=","+args[i];
		}
		
		System.out.println(new Options(r).toString());
	}
	
	Options(String opts) {
		Pattern p = Pattern.compile("([\\p{Alnum}_]+)=(.+)");
		for(String opt: opts.split(",")) {
			Matcher m = p.matcher(opt);
			boolean b = m.matches();

			if (!b) {
				options.put(opt,null);
			} else {
				options.put(m.group(1),m.group(2));
			}
		}
		keys = options.keySet();
	}
	
	boolean has(String opt) {
		return keys.contains(opt);
	}
	
	String value(String opt) {
		return options.get(opt);
	}

	public String toString() {
		String r = "";
		for(String k: keys) {
			r += k + "\n  " + options.get(k) + "\n";
		}
		return r;
	}
}
