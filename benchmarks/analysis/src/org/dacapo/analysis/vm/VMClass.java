package org.dacapo.analysis.vm;

import java.util.TreeMap;

public class VMClass {

	private VMClasses parent;
	private Long      tag       = null;
	private String    className = null;
	private long      time      = 0;

	private static TreeMap<Long,VMClass>   tagToClass       = new TreeMap<Long,VMClass>();
	private static TreeMap<String,VMClass> classNameToClass = new TreeMap<String,VMClass>(); 
	
	public VMClass(Long tag, String className, long time) {
		this.parent    = null;
		this.tag       = tag;
		this.className = className;
		this.time      = time;
	}
	
	VMClass(VMClasses parent) {
		this.parent    = parent;
	}
	
	public Long getTag() { return tag; }
	public void setTag(Long tag) {
		if (this.tag == tag) return;
		
		if (this.parent != null && this.tag != null) parent.unlinkTag(this);
		this.tag = tag;
		if (this.parent != null && this.tag != null) parent.linkTag(this);
	}
	
	public String getClassName() { return className; }
	public void setClassName(String className) {
		if (this.className == className) return;
		className = normalizeClassName(className);

		if (this.parent != null && this.className != null) parent.unlinkClassName(this);
		this.className = className;
		if (this.parent != null && this.className != null) parent.linkClassName(this);
	}
	
	public long getTime() { return time; }
	public void setTime(long time) { this.time = time; }
	
	public VMClasses getParent() { return parent; }
	public void setParent(VMClasses parent) {
		if (this.parent == parent) return;
		
		if (this.parent != null) parent.unlink(this);
		this.parent = parent;
		if (this.parent != null) parent.link(this);
	}
	
	public String toString() {
		return "Class["+tag+","+className+","+time+"]";
	}
	
	static String normalizeClassName(String className) {
		if (className.endsWith(";")) {
			assert className.startsWith("L");
			className = className.substring(1, className.length()-1);
		}
		if (className.indexOf('.')!=-1)
			return className.replace('.','/');
		return className;
	}
}
