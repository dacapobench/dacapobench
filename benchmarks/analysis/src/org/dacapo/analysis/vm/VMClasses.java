package org.dacapo.analysis.vm;

import java.util.Set;
import java.util.TreeMap;

public class VMClasses {

	private TreeMap<Long,VMClass>   tagToClass       = new TreeMap<Long,VMClass>();
	private TreeMap<String,VMClass> classNameToClass = new TreeMap<String,VMClass>();
	
	public VMClasses() {
		
	}
	
	public VMClass updateClass(Long tag, String className, long time) {
		return null;
	}

	public VMClass makeClass(Long tag, String className, long time) {
		assert tagToClass.get(tag) == null;
		className = VMClass.normalizeClassName(className);
		assert classNameToClass.get(className) == null;
		
		VMClass vmClass = new VMClass(this);
		
		vmClass.setTag(tag);
		vmClass.setClassName(className);
		
		return vmClass;
	}
	
	public Set<Long> getTags() { return tagToClass.keySet(); }
	
	public VMClass find(Long tag) { return tagToClass.get(tag); }

	public Set<String> getClassNames() { return classNameToClass.keySet(); }
	
	public VMClass find(String className) { return classNameToClass.get(VMClass.normalizeClassName(className)); }
	
	VMClass unlinkTag(VMClass vmClass) {
		return tagToClass.remove(vmClass.getTag());
	}
	void linkTag(VMClass vmClass) {
		VMClass old = tagToClass.put(vmClass.getTag(), vmClass);
		assert old == null;
	}
	
	VMClass unlinkClassName(VMClass vmClass) {
		return classNameToClass.remove(vmClass.getClassName());
	}
	void linkClassName(VMClass vmClass) {
		VMClass old = classNameToClass.put(vmClass.getClassName(), vmClass);
		assert old == null;
	}
	
	VMClass unlink(VMClass vmClass) {
		if (vmClass.getTag() != null) {
			VMClass old = tagToClass.remove(vmClass.getTag());
			assert old == vmClass;
		}
		if (vmClass.getClassName() != null) {
			VMClass old = classNameToClass.remove(vmClass.getClassName());
			assert old == vmClass;
		}
		return vmClass;
	}
	void link(VMClass vmClass) {
		linkTag(vmClass);
		linkClassName(vmClass);
	}
}
