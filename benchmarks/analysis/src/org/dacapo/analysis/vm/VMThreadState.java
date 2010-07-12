package org.dacapo.analysis.vm;

import java.util.TreeMap;

public class VMThreadState {

	public  final  static long   UNKNOWN = -1;
	
	private Long    tag;
	private long    start;
	private long    stop;
	private VMClass vmClass;
	private String  name;
	private long    totalExecutionTime;
	private TreeMap<Long,Long> slices = new TreeMap<Long,Long>();
	private VMThreadStates parent = null;
	
	private final static String EOL = System.getProperty("line.separator");
	
	public Long    getTag()       { return tag; }
	public void    setTag(Long tag) {
		if (this.tag == tag) return;
		
		if (this.parent != null && this.tag != null) parent.unlinkTag(this);
		this.tag = tag;
		if (this.parent != null && this.tag != null) parent.linkTag(this);
	}
	
	public VMClass getVMClass()   { return vmClass; }
	public void setVMClass(VMClass vmClass) { this.vmClass = vmClass; }
	
	public String  getName()      { return name; }
	public void setName(String name) {
		if (this.name == name) return;
		
		if (this.parent != null && this.name != null) parent.unlinkName(this);
		this.name = name;
		if (this.parent != null && this.name != null) parent.linkName(this);
	}
	
	public long   getStart()      { return start; }
	public void   setStart(long start) { this.start = start; }
	
	public long   getStop()       { return stop; }
	public void   setStop(long stop) { this.stop = stop; }
	
	public void   addTimeSlice(Long sliceTime, Long duration) {
		Long value = slices.get(sliceTime);
		if (value!=null) totalExecutionTime -= value;
		if (duration!=null && 0<duration) {
			slices.put(sliceTime, duration);
			totalExecutionTime += duration;
		} else {
			slices.remove(sliceTime);
		}
	}
	
	public TreeMap<Long,Long> getTimeSlices() { return slices; }

	public String toString() {
		StringBuffer str = new StringBuffer("Thread["+tag+","+name+","+vmClass+","+timeString(start)+","+timeString(stop)+","+totalExecutionTime+"]");
		
		for(Long time: slices.keySet()) {
			Long duration = slices.get(time);
			
			str.append(EOL+"  "+time+":"+duration);
		}
		
		return str.toString();
	}
	
	public VMThreadState(Long tag, VMClass vmClass, String name, long start) {
		this(tag, vmClass, name, start, UNKNOWN);
	}
	
	public VMThreadState(Long tag, VMClass vmClass, String name, long start, long stop) {
		this.tag       = tag;
		this.vmClass   = vmClass;
		this.name      = name;
		this.start     = start;
		this.stop      = stop;
		this.parent    = null;
	}

	public VMThreadStates getParent() { return parent; }
	public void setParent(VMThreadStates parent) {
		if (this.parent == parent) return;
		
		if (this.parent != null) parent.unlink(this);
		this.parent = parent;
		if (this.parent != null) parent.link(this);
	}
	
	
	private static String timeString(long time) {
		return time==UNKNOWN?"unknown":""+time;
	}
}
