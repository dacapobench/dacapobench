package org.dacapo.analysis.util;

import gr.spinellis.ckjm.ClassMetrics;

public class ClassMetricAccumulator {

	private int ca   = 0;
	private int cbo  = 0;
	private int dit  = 0;
	private int lcom = 0;
	private int noc  = 0;
	private int npm  = 0;
	private int rfc  = 0;
	private int wmc  = 0;
	
	public void add(ClassMetrics c) {
		ca   += c.getCa();
		cbo  += c.getCbo();
		dit  += c.getDit();
		lcom += c.getLcom();
		noc  += c.getNoc();
		npm  += c.getNpm();
		rfc  += c.getRfc();
		wmc  += c.getWmc();
	}
	
	public int getCa() { return ca; }
	public int getCbo() { return cbo; }
	public int getDit() { return dit; }
	public int getLcom() { return lcom; }
	public int getNoc() { return noc; }
	public int getNpm() { return npm; }
	public int getRfc() { return rfc; }
	public int getWmc() { return wmc; }
}
