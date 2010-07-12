package org.dacapo.analysis.vm;

import java.util.LinkedList;
import java.util.Set;
import java.util.TreeMap;

public class VMThreadStates {

	private TreeMap<Long,VMThreadState> tagToVMThreadState = new TreeMap<Long,VMThreadState>();
	private TreeMap<String,LinkedList<VMThreadState>> nameToVMThreadState = new TreeMap<String,LinkedList<VMThreadState>>(); 
	
	public VMThreadStates() { }
	
	public Set<Long> getTags() { return tagToVMThreadState.keySet(); }
		
	public VMThreadState find(Long tag) {
		return tagToVMThreadState.get(tag);
	}
	
	VMThreadState unlinkTag(VMThreadState vmThreadState) {
		return tagToVMThreadState.remove(vmThreadState.getTag());
	}
	void linkTag(VMThreadState vmThreadState) {
		assert vmThreadState != null;
		VMThreadState old = tagToVMThreadState.put(vmThreadState.getTag(),vmThreadState);
		assert old == null;
	}
	
	VMThreadState unlinkName(VMThreadState vmThreadState) {
		assert vmThreadState != null;
		LinkedList<VMThreadState> list = nameToVMThreadState.get(vmThreadState.getName());
		
		if (list != null) {
			list.remove(vmThreadState);
			if (list.isEmpty()) nameToVMThreadState.remove(vmThreadState.getName());
		}
		
		return vmThreadState;
	}
	void linkName(VMThreadState vmThreadState) {
		assert vmThreadState != null;
		LinkedList<VMThreadState> list = nameToVMThreadState.get(vmThreadState.getName());
		
		if (list==null) {
			list = new LinkedList<VMThreadState>();
			nameToVMThreadState.put(vmThreadState.getName(),list);
		}
		list.add(vmThreadState);
	}
	
	void unlink(VMThreadState vmThreadState) {
		unlinkTag(vmThreadState);
		unlinkName(vmThreadState);
	}
	void link(VMThreadState vmThreadState) {
		linkTag(vmThreadState);
		linkName(vmThreadState);
	}
}
