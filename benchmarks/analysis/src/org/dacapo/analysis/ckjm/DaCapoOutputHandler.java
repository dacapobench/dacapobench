package org.dacapo.analysis.ckjm;

import java.io.OutputStream;

import gr.spinellis.ckjm.*;

import org.dacapo.analysis.util.ClassMetricAccumulator;
import org.dacapo.util.CSVOutputStream;

public class DaCapoOutputHandler implements CkjmOutputHandler {

	private static final String    TOTAL = "TOTAL";

	private boolean                reportIndividualClasses;
	private CSVOutputStream              csv;
	private ClassMetricAccumulator metricTotal = new ClassMetricAccumulator();
	
	DaCapoOutputHandler(OutputStream os, boolean reportIndividualClasses) {
		this(new CSVOutputStream(os), reportIndividualClasses);
	}
	
	DaCapoOutputHandler(CSVOutputStream csv, boolean reportIndividualClasses) {
		this.csv = csv;
		this.reportIndividualClasses = reportIndividualClasses;
	}
	
	public synchronized void handleClass(java.lang.String name, ClassMetrics c) {
		if (reportIndividualClasses) {
			csv.write(name);
			csv.write(""+c.getCa());
			csv.write(""+c.getCbo());
			csv.write(""+c.getDit());
			csv.write(""+c.getLcom());
			csv.write(""+c.getNoc());
			csv.write(""+c.getNpm());
			csv.write(""+c.getRfc());
			csv.write(""+c.getWmc());
			csv.eol();
		}
		metricTotal.add(c);
	}
	
	public ClassMetricAccumulator getMetricTotal() { return metricTotal; }
	
	public synchronized void reportTotal() {
		csv.write(TOTAL);
		csv.write(""+metricTotal.getCa());
		csv.write(""+metricTotal.getCbo());
		csv.write(""+metricTotal.getDit());
		csv.write(""+metricTotal.getLcom());
		csv.write(""+metricTotal.getNoc());
		csv.write(""+metricTotal.getNpm());
		csv.write(""+metricTotal.getRfc());
		csv.write(""+metricTotal.getWmc());
		csv.eol();
	}
}
