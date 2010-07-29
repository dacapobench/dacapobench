package org.dacapo.analysis.memory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.LinkedList;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

import gr.spinellis.ckjm.*;
import org.apache.bcel.generic.Type;
import org.dacapo.analysis.util.events.Event;
import org.dacapo.analysis.util.events.EventAllocation;
import org.dacapo.analysis.util.events.EventClassPrepare;
import org.dacapo.analysis.util.events.EventErrorListener;
import org.dacapo.analysis.util.events.EventFree;
import org.dacapo.analysis.util.events.EventHeapReport;
import org.dacapo.analysis.util.events.EventListener;
import org.dacapo.analysis.util.events.EventParseException;
import org.dacapo.analysis.util.events.EventPointerChange;
import org.dacapo.analysis.util.events.EventStart;
import org.dacapo.analysis.util.events.EventStop;

import org.apache.bcel.Repository;
import org.apache.bcel.util.ClassPath;
import org.apache.bcel.util.SyntheticRepository;

import org.dacapo.instrument.LogTags;
import org.dacapo.util.CSVInputStream;
import org.dacapo.util.CSVOutputStream;
import org.dacapo.util.LogFiles;
import org.dacapo.util.CSVInputStream.CSVException;

public class Memory {
	
	private static final String CLASS_EXTENSION          = ".class";
	
	public static void main(String[] args) throws Exception {
		CommandLineArgs commandLine = new CommandLineArgs(args);
		
		LinkedList<File> fileList = LogFiles.orderedLogFileList(commandLine.getLogFile());
		
		AllocationListener listener = new AllocationListener(new CSVOutputStream(commandLine.getOutputStream()), commandLine.getAll());

		listener.processStart();

		EventErrorListener eventErrorListener = new EventErrorListener() {
			public void handle(EventParseException e) throws EventParseException { System.err.println(e); }
			public void handle(CSVException e) throws CSVException { System.err.println(e); }
		};
		
		for(File file: fileList) {
			CSVInputStream is = new CSVInputStream(new FileInputStream(file));

			Event.parseEvents(listener, is, eventErrorListener);
		}
		
		listener.processStop();
	}

	private final static class Allocation {
		private AllocationListener allocationListener;
		private Long               tag;
		private String             className;
		private long               totalAllocation   = 0;
		private long               currentAllocation = 0;
		private long               totalObjects      = 0;
		private long               currentObjects    = 0;
		private long               negativeTagCount  = 0;
		
		public Allocation(AllocationListener allocationListener, Long tag, String className) {
			assert allocationListener != null;
			assert tag != null;
			assert className != null;
			
			this.allocationListener = allocationListener;
			this.tag                = tag;
			this.className          = className;
		}
		
		public void allocate(Long size) {
			this.totalAllocation   += size;
			this.currentAllocation += size;
			this.totalObjects      ++;
			this.currentObjects    ++;
			this.allocationListener.allocate(size);
		}
		
		public void deallocate(Long size) {
			this.currentAllocation -= size;
			this.currentObjects    --;
			this.allocationListener.deallocate(size);
		}
		
		public void addObjectTag(Long objectTag) {
			if (objectTag != null && objectTag < 0) {
				negativeTagCount++;
				this.allocationListener.addObjectTag(objectTag);
			}
		}
		
		public long getClassTag() { return this.tag; }
		
		public String getClassName() { return this.className; }
		
		public void setClassName(String className) { this.className = className; }
		
		public long getTotalAllocation() { return this.totalAllocation; }
		
		public long getCurrentAllocation() { return this.currentAllocation; }
		
		public static void writeHeadings(CSVOutputStream os) {
			os.write("Type");
			os.write("ClassName");
			os.write("Total Bytes");
			os.write("Current Bytes");
			os.write("Negative Tag Count");
			os.write("Total Objects");
			os.write("Current Objects");
			os.write("Pointer Mutations");
			os.eol();
		}
		
		public void write(CSVOutputStream os) {
			if (this.className.startsWith("["))
				os.write("ARRAY");
			else
				os.write("OBJECT");
			os.write(this.className);
			os.write(""+this.totalAllocation);
			os.write(""+this.currentAllocation);
			os.write(""+this.negativeTagCount);
			os.write(""+this.totalObjects);
			os.write(""+this.currentObjects);
			os.eol();
		}
	}
	
	private final static class Pair {
		public Allocation type;
		public Long       size;
		
		public Pair(Allocation type, Long size) {
			this.type = type;
			this.size = size;
		}
	}
	
	private final static class AllocationListener extends EventListener {
		long totalAllocation   = 0;
		long currentAllocation = 0;
		long totalObjects      = 0;
		long currentObjects    = 0;
		long negativeTagCount  = 0;
		long pointerMutations  = 0;

		TreeMap<Long,String>      tagToClass     = new TreeMap<Long,String>();
		TreeMap<String,Long>      classToTag     = new TreeMap<String,Long>();
		TreeMap<Long,Allocation>  allocation     = new TreeMap<Long,Allocation>();
		TreeMap<Long,Pair>        objectAlloc    = new TreeMap<Long,Pair>();
	
		CSVOutputStream os;
		boolean all;
		
		public AllocationListener(CSVOutputStream os, boolean all) {
			this.os = os;
			this.all = all;
		}
		
		public void processStart() { }
		
		public void processEvent(Event e) {
			if (e.getLogPrefix() == LogTags.LOG_PREFIX_CLASS_PREPARE) {
				EventClassPrepare eCP = (EventClassPrepare)e;

				map(eCP.getClassTag(), eCP.getClassName());
			} else if (all || getLogOn()) {
				if (e.getLogPrefix() == LogTags.LOG_PREFIX_ALLOCATION) {
					EventAllocation eA = (EventAllocation)e;
					
					Allocation klassAlloc = getAllocation(eA.getAllocClassTag(), eA.getAllocClass());
					klassAlloc.allocate(eA.getSize());
					Pair p = new Pair(klassAlloc,eA.getSize());
					this.objectAlloc.put(eA.getObjectTag(),p);

					klassAlloc.addObjectTag(eA.getObjectTag());
				} else if (e.getLogPrefix() == LogTags.LOG_PREFIX_FREE) {
					EventFree eF = (EventFree)e;
					
					Pair p = this.objectAlloc.remove(eF.getObjectTag());
					
					if (p != null)
						p.type.deallocate(p.size);
				} else if (e.getLogPrefix() == LogTags.LOG_PREFIX_POINTER) {
					EventPointerChange ePC = (EventPointerChange)e;
					
					map(ePC.getObjectClassTag(), ePC.getObjectClassName());
					map(ePC.getBeforeClassTag(), ePC.getBeforeClassName());
					map(ePC.getAfterClassTag(),  ePC.getAfterClassName());

					// do some stuff with pointer change here
					
					pointerMutations ++;
				}
			} else if (e.getLogPrefix() == LogTags.LOG_PREFIX_ALLOCATION) {
				EventAllocation eA = (EventAllocation)e;

				// just ensure we have mappings
				map(eA.getAllocClassTag(), eA.getAllocClass());
				getAllocation(eA.getAllocClassTag(), eA.getAllocClass());
			}
		}
		
		private void map(Long tag, String name) {
			if (tag == null || tag == 0) return;
			
			if (tagToClass.containsKey(tag)) {
				String className = tagToClass.get(tag);
				if ((className == null || className.isEmpty()) && name != null)
					tagToClass.put(tag, name);
			} else {
				tagToClass.put(tag, name);
			}

			if (name != null && !name.isEmpty() && !classToTag.containsKey(name)) {
				classToTag.put(name, tag);
			}
		}
		
		private Allocation getAllocation(Long classTag, String className) {
			Allocation klassAlloc = this.allocation.get(classTag);
			if (klassAlloc == null) {
				klassAlloc = new Allocation(this, classTag, className);
				this.allocation.put(classTag, klassAlloc);
			} else {
				if (klassAlloc.getClassName().isEmpty() && className!=null && !className.isEmpty()) {
					klassAlloc.setClassName(className);
				}
			}
			return klassAlloc;
		}
		
		public void processStop() {
			if (this.os != null) {
				Allocation.writeHeadings(this.os);
				
				AbstractMap<String, LinkedList<Allocation>> map = new TreeMap<String, LinkedList<Allocation>>();
				
				for(Allocation alloc: allocation.values()) {
					String className = tagToClass.get(alloc.getClassTag());
					
					if (className == null) {
						className = alloc.getClassName();
					} else if (alloc.getClassName().isEmpty()) {
						alloc.setClassName(className);
					}
					
					LinkedList<Allocation> list = map.get(className);
					
					if (list == null) {
						list = new LinkedList<Allocation>();
						map.put(alloc.getClassName(), list);
					}
					list.add(alloc);
				}
				
				for(String key: map.keySet()) {
					LinkedList<Allocation> list = map.get(key);
					for(Allocation alloc: list)
						alloc.write(this.os);
				}
				
				os.write("TOTAL");
				os.write("");
				os.write(""+this.totalAllocation);
				os.write(""+this.currentAllocation);
				os.write(""+this.negativeTagCount);
				os.write(""+this.totalObjects);
				os.write(""+this.currentObjects);
				os.write(""+this.pointerMutations);
				os.eol();
			}
		}
		
		public void allocate(Long size) {
			totalAllocation   += size;
			currentAllocation += size;
			totalObjects      ++;
			currentObjects    ++;
		}
		
		public void deallocate(Long size) {
			currentAllocation -= size;
			currentObjects    --;
		}
		
		public void addObjectTag(Long objectTag) {
			if (objectTag < 0) 
				negativeTagCount --;
		}
	}

}