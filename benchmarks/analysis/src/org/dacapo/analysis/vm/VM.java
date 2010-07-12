package org.dacapo.analysis.vm;

public class VM {

	VMClasses      vmClasses = new VMClasses();
	VMThreadStates vmThreadStates = new VMThreadStates();
	
	public VM() {
		
	}
	
	public VMClasses getVMClasses() { return vmClasses; }
	
	public VMThreadStates getVMThreadStates() { return vmThreadStates; }
}
