package dacapo.chart;

import org.jfree.data.category.*;
import org.jfree.data.xy.*;


import java.io.*;
import java.util.*;
import java.net.URL;

public abstract class Datasets implements GraphConstants {
  
  private static int positiveSeriesCount = 0;
  private static int negativeSeriesCount = 0;

  /**
   * Get a histogram dataset
   */
  public static CategoryDataset getHistDataset(URL fromFile, int chartType,
					       int withTimeline) 
    throws Exception {
    return getHistDataset(fromFile, chartType, withTimeline, RANGE_IN_MB);
  }
  
  /**
   * Get a histogram dataset
   */
  public static CategoryDataset getHistDataset(URL fromFile, int chartType, 
					       int withTimeline, int rangeIn) 
    throws Exception {

    double[][] result = null;

    switch (chartType) {
      // Histograms
    case PERFECT_PTR_DIST_HIST:
      result = createPointerDistHistData(fromFile, true, false, withTimeline);
      return createCategoryDataset("", "", result);
    case PERFECT_PTR_SRC_HIST:
      result = createPtrAgeHistData(fromFile, true, true, false, withTimeline);
      return createCategoryDataset("", "", result);
    case PERFECT_PTR_TGT_HIST:
      result = createPtrAgeHistData(fromFile, false, true, false, withTimeline);
      return createCategoryDataset("", "", result);
    case PERFECT_MUT_DIST_HIST:
      result = createPointerDistHistData(fromFile, true, true, withTimeline);
      return createCategoryDataset("", "", result);
    case PERFECT_MUT_SRC_HIST:
      result = createPtrAgeHistData(fromFile, true, true, true, withTimeline);
      return createCategoryDataset("", "", result);
    case PERFECT_MUT_TGT_HIST:
      result = createPtrAgeHistData(fromFile, false, true, true, withTimeline);
      return createCategoryDataset("", "", result);
    case PERFECT_HEAP_COMP_HIST:
      result = createHeapHistData(fromFile, rangeIn);
      return createCategoryDataset("", "", result);
    case PERFECT_INV_HEAP_COMP_HIST:
      result = createHeapHistData(fromFile, rangeIn);
      return createCategoryDataset("", "", result);
    case CUMULATIVE_PTR_DIST_HIST:
      result = createPointerDistHistData(fromFile, false, false, withTimeline);
      return createCategoryDataset("", "", result);
    case CUMULATIVE_PTR_SRC_HIST:
      result = createPtrAgeHistData(fromFile, true, false, false, withTimeline);
      return createCategoryDataset("", "", result);
    case CUMULATIVE_PTR_TGT_HIST:
      result = createPtrAgeHistData(fromFile, false, false, false, withTimeline);
    case CUMULATIVE_MUT_DIST_HIST:
      result = createPointerDistHistData(fromFile, false, true, withTimeline);
      return createCategoryDataset("", "", result);
    case CUMULATIVE_MUT_SRC_HIST:
      result = createPtrAgeHistData(fromFile, true, false, true, withTimeline);
      return createCategoryDataset("", "", result);
    case CUMULATIVE_MUT_TGT_HIST:
      result = createPtrAgeHistData(fromFile, false, false, true, withTimeline);
      return createCategoryDataset("", "", result);
    default: System.out.println("getHistDataset: No such graph type"); 
      break;
    }
    return null;
  }
  
  /**
   * Get a line-graph dataset
   */
  public static XYSeriesCollection getLineDataset(URL fromFile, 
						  int forChartType, 
						  int withTimeline)
    throws Exception{
    return getLineDataset(fromFile, forChartType, withTimeline, RANGE_IN_MB);
  }
  
  /**
   * Get a line-graph dataset
   */
  public static XYSeriesCollection getLineDataset(URL fromFile, 
						  int forChartType, 
						  int withTimeline, 
						  int rangeIn) 
    throws Exception {
    
    switch (forChartType) {
      // XY-Line graphs
    case PERFECT_PTR_DIST_XYPLOT:
      return createPointerDistData(fromFile, true, false, withTimeline);
    case PERFECT_PTR_SRC_XYPLOT:
      return createPointerAgeData(fromFile, true, true, false, withTimeline);
    case PERFECT_PTR_TGT_XYPLOT:
      return createPointerAgeData(fromFile, false, true, false, withTimeline);
    case PERFECT_MUT_DIST_XYPLOT:
      return createPointerDistData(fromFile, true, true, withTimeline);
    case PERFECT_MUT_SRC_XYPLOT:
      return createPointerAgeData(fromFile, true, true, true, withTimeline);
    case PERFECT_MUT_TGT_XYPLOT:
      return createPointerAgeData(fromFile, false, true, true, withTimeline);
    case PERFECT_HEAP_COMP_XYPLOT:
      return createHeapData(fromFile, false, withTimeline, rangeIn);
    case PERFECT_INV_HEAP_COMP_XYPLOT:
      return createHeapData(fromFile, true, withTimeline, rangeIn);
    case CUMULATIVE_PTR_DIST_XYPLOT:
      return createPointerDistData(fromFile, false, false, withTimeline);
    case CUMULATIVE_PTR_SRC_XYPLOT:
      return createPointerAgeData(fromFile, true, false, false, withTimeline);
    case CUMULATIVE_PTR_TGT_XYPLOT:
      return createPointerAgeData(fromFile, false, false, false, withTimeline);
    case CUMULATIVE_MUT_DIST_XYPLOT:
      return createPointerDistData(fromFile, false, true, withTimeline);
    case CUMULATIVE_MUT_SRC_XYPLOT:
      return createPointerAgeData(fromFile, true, false, true, withTimeline);
    case CUMULATIVE_MUT_TGT_XYPLOT:
      return createPointerAgeData(fromFile, false, false, true, withTimeline);
    default: System.out.println("getLineDateset: No such graph type"); 
      break;
    }
    return null;
  }

  
  private static CategoryDataset createCategoryDataset(String rowKeyPrefix,
						       String columnKeyPrefix,
						       double[][] data) {

    DefaultCategoryDataset result = new DefaultCategoryDataset();
    for (int r = 0; r < data.length; r++) {
      String rowKey = rowKeyPrefix + r;
      for (int c = 0; c < data[r].length; c++) {
	String columnKey = columnKeyPrefix + c;
	result.addValue(new Double(data[r][c]), rowKey, columnKey);
      }
    }
    return result;
  }

  
  public static double[][] createHeapHistData(URL inURL, int rangeIn)
    throws IOException {

    BufferedReader file = new BufferedReader(new InputStreamReader(inURL.openStream()));
    
    int index = 0;
    int currCohortCount = 0;
    String line = null;
    String startTag, endTag;
    StringTokenizer st = null;
    
    int maxCohort = getMaxCohorts(inURL); // index starts at 1
    double[] cohortHist = new double[maxCohort];
    
    int denom = 1;
    
    switch (rangeIn) {
    case RANGE_IN_KB:
      denom = 1024; break;
    case RANGE_IN_MB:
      denom = 1024*1024; break;
    default: break;
    }
    
    double[] cohortVolumes;
    
    startTag = "<cohort data>";
    endTag = "</cohort data>";
    
    while((line=file.readLine()) != null) {
      if (line.equals("<cohort data>")) {
	line = file.readLine(); // goto the next line
	line = file.readLine(); // goto the next line
	st = new StringTokenizer(line, ",");
	st.nextToken();
	st.nextToken(","); // ignore
	currCohortCount = Integer.parseInt(st.nextToken(",")); // cohorts
	
	cohortVolumes = new double[currCohortCount];
	line = file.readLine(); // goto the next line
	do {
	  st = new StringTokenizer(line, ",");
	  index = Integer.parseInt(st.nextToken());
	  st.nextToken(","); // ignore
	  cohortVolumes[index] = Double.parseDouble(st.nextToken(","));
	  line = file.readLine();
	} while(!line.equals(endTag));
	
	for(int i=0;i < currCohortCount; i++) {
	  cohortHist[i] = (cohortVolumes[i]==0)?0:cohortVolumes[i]/denom;
	}
      }
    }
    return new  double[][]{cohortHist};
  }
  
  
  private static double[][] createPointerDistHistData(URL inURL,
						      boolean isPerfect,
						      boolean isMutation,
						      int timeline)
    throws IOException {

    BufferedReader file = new BufferedReader(new InputStreamReader(inURL.openStream()));

    String temp = null;
    String line = null;
    StringTokenizer st;
    
    int posBuckets = 32; // 31 + extra
    int negBuckets = 32; // 31 + extra
    
    double[] positive = null; int totalPositive = 0; double sum = 0;
    double[] negative = null; int totalNegative = 0;
    int maxPosFound = 0, maxNegFound = 0;
    double totalZero = 0;

    double[] totalPos = new double[posBuckets];
    double[] totalNeg = new double[negBuckets];
    
    double totalMutations = 0;
    double totalAllocs = 0;
    double currentMutations = 0;
    boolean isTimeAllocs = false;
    
    positive = new double[posBuckets]; //  0   -> 2^30
    negative = new double[negBuckets]; // -2^1 -> -2^30
    double zeroDistance = 0.0;
    
    String mutOrPoint = ((isMutation) ? "mutation " : "pointer ");
    String primaryStartTag = "<"+mutOrPoint+"data>";
    String primaryEndTag = "</"+mutOrPoint+"data>";
    String baseTag = ((isPerfect) ? "perfect " : "") + mutOrPoint + "dist";
    String startTag = "<"+baseTag+">";
    String endTag = "</"+baseTag+">";
    
    switch (timeline) {
    case TIMELINE_MUTATIONS:
      isTimeAllocs = false; break;
    case TIMELINE_ALLOCATIONS:
      isTimeAllocs = true; break;
    default: break;
    }
    
    // How many buckets to read
    final int READ_POS_BUCKETS = posBuckets;
    final int READ_NEG_BUCKETS = negBuckets;
    int dataPoint = 0;
    
    // Donot change these. Instead, change READ_POS/NEG_BUCKETS
    int read_pos_buckets = READ_POS_BUCKETS;
    int read_neg_buckets = READ_NEG_BUCKETS;
    
    while((line=file.readLine()) != null) {
      if (line.equals(primaryStartTag)) {
	line = file.readLine(); // goto the next line
	st = new StringTokenizer(line, ",");
        totalMutations = Double.parseDouble(st.nextToken());
	totalAllocs = Double.parseDouble(st.nextToken(",")); // time
        temp=file.readLine();
	while (!temp.equals(startTag)) {
	  temp=file.readLine();
	}
	
	// Read Source Distances
	temp = file.readLine(); // go to next line
	do {
	  st = new StringTokenizer(temp, ",");
	  int index = Integer.parseInt(st.nextToken());
          double value = Double.parseDouble(st.nextToken(","));
          if (index == 0) 
            zeroDistance = value;
          else if (index < 0)
            negative[-index-1] = value;
          else 
            positive[index-1] = value;

	  temp=file.readLine();
	} while(!temp.equals(endTag));

        sum = zeroDistance;
        totalZero += zeroDistance;
	for(int i=0; i<read_pos_buckets; i++) {
	  sum += positive[i];
	  totalPos[i] += positive[i];
	}
	for(int i=0; i<read_neg_buckets; i++) {
	  sum += negative[i];
	  totalNeg[i] += negative[i];
	}

	// reset arrays
	for(int i=0; i<read_pos_buckets; i++) {
	  positive[i] = 0;
	}
	for(int i=0; i<read_neg_buckets; i++) {
	  negative[i] = 0;
	}
	
	totalPositive = 0;
	totalNegative = 0;
	sum = 0;
	
	// reset in case values changed
	read_pos_buckets = READ_POS_BUCKETS;
	read_neg_buckets = READ_NEG_BUCKETS;
      }
    }
    
    double[] posVals = new double[totalPos.length+1];
    posVals[0] = ((totalZero/totalMutations)*100)/2;
    for(int i=1;i<posVals.length;i++) {
      posVals[i] = (totalPos[i-1]/totalMutations)*100;
    }
    
    double[] negVals = new double[totalNeg.length+1];
    negVals[0] = -((totalZero/totalMutations)*100)/2;
    for(int i=1;i<negVals.length;i++) {
      negVals[i] = -(totalNeg[i-1]/totalMutations)*100;
    }
    return new double[][]{posVals, negVals};
  }
  
  public static XYSeriesCollection createHeapData(URL inURL,
						  boolean invert,
						  int timeLine,
						  int rangeIn)
    throws IOException {

    BufferedReader file = new BufferedReader(new InputStreamReader(inURL.openStream()));
    
    int index = 0;
    int currCohortCount = 0;
    double totalAllocs = 0.0;
    String line = null;
    String startTag, endTag;
    StringTokenizer st = null;
    
    int maxCohort = getMaxCohorts(inURL); // index starts at 1
    double[] cohortHist = new double[maxCohort];
    
    double totalMutations = 0;
    boolean isTimeAllocs = false;
    int denom = 1;
    
    XYSeriesCollection dataset = new XYSeriesCollection();
    XYSeries[] cohorts = new XYSeries[maxCohort];
    double[] cohortVolumes;
    
    startTag = "<cohort data>";
    endTag = "</cohort data>";
    
    switch (timeLine) {
    case TIMELINE_MUTATIONS:
      isTimeAllocs = false; break;
    case TIMELINE_ALLOCATIONS:
      isTimeAllocs = true; break;
    default: break;
    }
    
    switch (rangeIn) {
    case RANGE_IN_KB:
      denom = 1024; 
      break;
    case RANGE_IN_MB:
      denom = 1024*1024; 
      break;
    default: break;
    }
    
    while((line=file.readLine()) != null) {
      if (line.equals("<cohort data>")) {
	line = file.readLine(); // goto the next line
	st = new StringTokenizer(line, ",");
        totalMutations = Double.parseDouble(st.nextToken());
	totalAllocs = Double.parseDouble(st.nextToken(",")); // time
	line = file.readLine(); // goto the next line
	st = new StringTokenizer(line, ",");
	st.nextToken();
	st.nextToken(","); // ignore
	currCohortCount = Integer.parseInt(st.nextToken(",")); // cohorts
	
	cohortVolumes = new double[currCohortCount];
	line = file.readLine(); // goto the next line
	do {
	  st = new StringTokenizer(line, ",");
	  index = Integer.parseInt(st.nextToken());
	  st.nextToken(","); // ignore obj count
	  cohortVolumes[index] = Double.parseDouble(st.nextToken(","));
	  line = file.readLine();
	} while(!line.equals(endTag));
	
	// Calculate cumulative cohort volumes
	double[] tempVolumes = new double[currCohortCount];
	for(int i=0; i<currCohortCount; i++) {
	  for(int j=currCohortCount-1; j>=i; j--) {
	    tempVolumes[i] += cohortVolumes[j];
	  }
	}
	
	// add values to XYSeries
	int cp = 0;
	for(int i=0;i < currCohortCount; i++) {
	  if (cohorts[i] == null) {
	    cohorts[i]= new XYSeries("Cohort " + (i+1));
	  }
	  cp += cohortVolumes[i];
	  if (!isTimeAllocs) {
	    if (invert) {
	      cohorts[i].add(totalMutations/1000000, cp/denom);
	    } else {
	      cohorts[i].add(totalMutations/1000000,tempVolumes[i]/denom);
	    }
	  } else {
	    if (invert) {
	      cohorts[i].add(totalAllocs, cp/denom);
	    } else {
	      cohorts[i].add(totalAllocs, tempVolumes[i]/denom);
	    }
	  }
	  if (!invert) {
	    cohortHist[i] = (cohortVolumes[i]==0)?0:cohortVolumes[i]/denom;
	  }
	}
      }
    }
    
    // add XYSeries to the collection
    int i = 0;
    while (i < maxCohort && cohorts[i] != null) {
      dataset.addSeries(cohorts[i]);
      i++;
    }

    return dataset;
  }
  
  
  private static XYSeriesCollection createPointerDistData(URL inURL,
							  boolean isPerfect,
							  boolean isMutation,
							  int timeline) 
    throws IOException {

    BufferedReader file = new BufferedReader(new InputStreamReader(inURL.openStream()));

    String temp = null;
    String line = null;
    StringTokenizer st;
    
    int posBuckets = 32; // 31 + extra
    int negBuckets = 32; // 31 + extra
    
    double[] positive = null; 
    double sum = 0;
    double[] negative = null; 

    int totalPositive = 0; 
    int totalNegative = 0;
    int maxPosFound = 0;
    int maxNegFound = 0;
    
    XYSeriesCollection dataset = new XYSeriesCollection();      
    XYSeries[] posdistances = new XYSeries[posBuckets];
    XYSeries[] negdistances = new XYSeries[negBuckets];
    
    double totalMutations = 0;
    double totalAllocs = 0;
    double currentMutations = 0;
    boolean isTimeAllocs = false;
    
    positive = new double[posBuckets]; //  0   -> 2^30
    negative = new double[negBuckets]; // -2^1 -> -2^30
    double zeroDistance = 0.0;

    String mutOrPoint = ((isMutation) ? "mutation " : "pointer ");
    String primaryStartTag = "<"+mutOrPoint+"data>";
    String primaryEndTag = "</"+mutOrPoint+"data>";
    String baseTag = ((isPerfect) ? "perfect " : "") + mutOrPoint + "dist";
    String startTag = "<"+baseTag+">";
    String endTag = "</"+baseTag+">";

    switch (timeline) {
    case TIMELINE_MUTATIONS:
      isTimeAllocs = false; break;
    case TIMELINE_ALLOCATIONS:
      isTimeAllocs = true; break;
    default: break;
    }
    
    // How many buckets to read
    final int READ_POS_BUCKETS = posBuckets;
    final int READ_NEG_BUCKETS = negBuckets;
    int dataPoint = 0;
    
    
    // Donot change these. Instead, change READ_POS/NEG_BUCKETS
    int read_pos_buckets = READ_POS_BUCKETS;
    int read_neg_buckets = READ_NEG_BUCKETS;
    
    for(int i=0; i<posBuckets; i++) {
      posdistances[i] = null;
    }
    for(int i=0; i<negBuckets; i++) {
      negdistances[i] = null;
    }
    
    while((line=file.readLine()) != null) {
      if (line.equals(primaryStartTag)) {
	line = file.readLine(); // goto the next line
	st = new StringTokenizer(line, ",");
        totalMutations = Double.parseDouble(st.nextToken());
	totalAllocs = Double.parseDouble(st.nextToken(",")); // time
        temp=file.readLine();
	while (!temp.equals(startTag)) {
	  temp=file.readLine();
	}
	
	// Read Source Distances
	temp = file.readLine(); // go to next line
	do {
	  st = new StringTokenizer(temp, ",");
	  int index = Integer.parseInt(st.nextToken());
          double value = Double.parseDouble(st.nextToken(","));
          if (index == 0) 
            zeroDistance = value;
          else if (index < 0)
            negative[-index-1] = value;
          else 
            positive[index-1] = value;

	  temp=file.readLine();
	} while(!temp.equals(endTag));

        sum = zeroDistance;
	for(int i=0; i<read_pos_buckets; i++) {
	  sum += positive[i];
	}
	for(int i=0; i<read_neg_buckets; i++) {
	  sum += negative[i];
	}
	
	double cp = ((zeroDistance/sum)*100)/2;
	// add positive dists to XYSeries
	for(int i=0; i<positive.length; i++) {
	  if (posdistances[i] == null) {
	    posdistances[i]= new XYSeries("+ve Bucket " + i);
	  }
          cp += (positive[i]/sum)*100;
	  if (!isTimeAllocs) {
	    posdistances[i].add(totalMutations/1000000, cp);
	  } else {
	    posdistances[i].add(totalAllocs, cp);
	  }
	}
	
	
	// add negative dists to XYSeries
	cp = -((zeroDistance/sum)*100)/2;
	for(int i=0; i<negative.length; i++) {
	  if (negdistances[i] == null) {
	    negdistances[i]= new XYSeries("-ve Bucket " + i);
	  }
          cp -= (negative[i]/sum)*100;
	  if (!isTimeAllocs) {
	    negdistances[i].add(totalMutations/1000000, cp);
	  } else {
	    negdistances[i].add(totalAllocs, cp);
	  }
	}
	
	// reset arrays
	for(int i=0; i<read_pos_buckets; i++) positive[i] = 0;
	for(int i=0; i<read_neg_buckets; i++) negative[i] = 0;
	zeroDistance = 0;

	totalPositive = 0;
	totalNegative = 0;
	sum = 0;
	
	// reset in case values changed
	read_pos_buckets = READ_POS_BUCKETS;
	read_neg_buckets = READ_NEG_BUCKETS;
      }
    }
    
    // add XYSeries to the collection
    for(int i=0;i<read_pos_buckets;i++) {
      dataset.addSeries(posdistances[i]);
    }
    
    // add XYSeries to the collection
    for(int i=0;i<read_neg_buckets;i++) {
      dataset.addSeries(negdistances[i]);
    }

    positiveSeriesCount = maxPosFound;
    negativeSeriesCount = maxNegFound;

    return dataset;
  }
  
  private static XYSeriesCollection createPointerAgeData(URL inURL,
							 boolean isSource,
							 boolean isPerfect,
                                                         boolean isMutation,
							 int timeline) 
    throws IOException {
    BufferedReader file = new BufferedReader(new InputStreamReader(inURL.openStream()));

    String line = null;
    String temp = null;
    StringTokenizer st;
    
    int maxAges = 32;
    
    double[] ages = null; 
    int totalAges = 0; 
    double sum = 0;
    
    XYSeriesCollection dataset = new XYSeriesCollection();
    XYSeries[] ptrages = new XYSeries[maxAges];
    int maxFound = 0;
    
    double[] mutationHistory = new double[maxAges];
    
    double totalMutations = 0;
    double totalAllocs = 0;
    double currentMutations = 0;
    boolean isTimeAllocs = false;
    
    String mutOrPoint = ((isMutation) ? "mutation " : "pointer ");
    String primaryStartTag = "<"+mutOrPoint+"data>";
    String primaryEndTag = "</"+mutOrPoint+"data>";
    String baseTag = ((isPerfect) ? "perfect " : "") + mutOrPoint
      + ((isSource) ? "src" : "tgt");
    String startTag = "<"+baseTag+">";
    String endTag = "</"+baseTag+">";
    
    switch (timeline) {
    case TIMELINE_MUTATIONS:
      isTimeAllocs = false; break;
    case TIMELINE_ALLOCATIONS:
      isTimeAllocs = true; break;
    default: break;
    }
    
    ages = new double[maxAges]; // 0 -> 2^30
    
    
    //NO NEED FOR THIS
    // we'll initialize them later.. just to make sure they're all null
    for(int i=0; i<maxAges; i++) {
      ptrages[i] = null;
    }
    
    // How many buckets to read
    final int READ_AGE_BUCKETS = maxAges;
    
    // Do not change this, instead change READ_AGE_BUCKETS
    int read_age_buckets = READ_AGE_BUCKETS;
    
    while((line=file.readLine()) != null) {
      if (line.equals(primaryStartTag)) {
	line = file.readLine(); // goto the next line
	st = new StringTokenizer(line, ",");
        totalMutations = Double.parseDouble(st.nextToken());
	totalAllocs = Double.parseDouble(st.nextToken(",")); // time
        temp=file.readLine();
	while (!temp.equals(startTag)) {
	  temp=file.readLine();
	}
	
	// Read Source Distances
	temp = file.readLine(); // go to next line
	do {
	  ++totalAges;
	  st = new StringTokenizer(temp, ",");
	  int index = Integer.parseInt(st.nextToken());
	  ages[index]= Double.parseDouble(st.nextToken(","));
	  temp=file.readLine();
	  maxFound=(totalAges>maxFound)?totalAges:maxFound;
	  if (totalAges == read_age_buckets) {
	    break;
	  }

	} while(!temp.equals(endTag));
	
	// read_age_buckets can be less than READ_AGE_BUCKETS
	// when totalAges is less, but it cannot be more than totalAges
	read_age_buckets = maxFound;
	
	for(int i=0; i<read_age_buckets; i++) {
	  sum += ages[i];
	  mutationHistory[i] += ages[i];
	}
	
	double cp = 0;
	// add positive dists to XYSeries
	for(int i=0; i<read_age_buckets; i++) {
	  if (ptrages[i] == null) {
	    ptrages[i]= new XYSeries("Bucket " + (i+1));
	  }
	  cp += (ages[i]/sum)*100;
	  if (!isTimeAllocs) {
	    ptrages[i].add(totalMutations/1000000, cp);
	  } else {
	    ptrages[i].add(totalAllocs, cp);
	  }
	}
	
	for(int i=0; i<read_age_buckets; i++) ages[i] = 0;
	totalAges = 0;
	sum = 0;
	// in case read_age_buckets is less than READ_AGE_BUCKETS
	read_age_buckets = READ_AGE_BUCKETS;
      }
    }
    
    double[] ageHist = new double[maxFound];
    // add XYSeries to the collection
    for(int i=0;i<read_age_buckets;i++) {
      String series = new String("Bucket " +(i+1));
      if(ptrages[i] == null) {
	continue;
      }
      ageHist[i] = (mutationHistory[i]/totalMutations)*100;
      dataset.addSeries(ptrages[i]);
    }
    
    return dataset;
  }
  
  
  
  private static double[][] createPtrAgeHistData(URL inURL,
						 boolean isSource,
						 boolean isPerfect,
                                                 boolean isMutation,
						 int timeline) 
    throws IOException {
    BufferedReader file = new BufferedReader(new InputStreamReader(inURL.openStream()));

    String line = null;
    String temp = null;
    StringTokenizer st;
    
    int maxAges = 32;
    
    double[] ages = null; int totalAges = 0; double sum = 0;
    
    XYSeriesCollection dataset = new XYSeriesCollection();
    XYSeries[] ptrages = new XYSeries[maxAges];
    int maxFound = 0;
    
    double[] mutationHistory = new double[maxAges];
    
    double totalMutations = 0;
    double totalAllocs = 0;
    double currentMutations = 0;
    boolean isTimeAllocs = false;
    
    String mutOrPoint = ((isMutation) ? "mutation " : "pointer ");
    String primaryStartTag = "<"+mutOrPoint+"data>";
    String primaryEndTag = "</"+mutOrPoint+"data>";
    String baseTag = ((isPerfect) ? "perfect " : "") + mutOrPoint
      + ((isSource) ? "src" : "tgt");
    String startTag = "<"+baseTag+">";
    String endTag = "</"+baseTag+">";
    
    switch (timeline) {
    case TIMELINE_MUTATIONS:
      isTimeAllocs = false; break;
    case TIMELINE_ALLOCATIONS:
      isTimeAllocs = true; break;
    default: break;
    }
    
    ages = new double[maxAges]; // 0 -> 2^30
    
    
    //NO NEED FOR THIS
    // we'll initialize them later.. just to make sure they're all null
    for(int i=0; i<maxAges; i++) {
      ptrages[i] = null;
    }
    
    // How many buckets to read
    final int READ_AGE_BUCKETS = maxAges;
    
    // Do not change this, instead change READ_AGE_BUCKETS
    int read_age_buckets = READ_AGE_BUCKETS;
    
    while((line=file.readLine()) != null) {
      if (line.equals(primaryStartTag)) {
	line = file.readLine(); // goto the next line
	st = new StringTokenizer(line, ",");
        totalMutations = Double.parseDouble(st.nextToken());
	totalAllocs = Double.parseDouble(st.nextToken(",")); // time
        temp=file.readLine();
	while (!temp.equals(startTag)) {
	  temp=file.readLine();
	}
	
	// Read Source Distances
	temp = file.readLine(); // go to next line
	do {
	  ++totalAges;
	  st = new StringTokenizer(temp, ",");
	  int index = Integer.parseInt(st.nextToken());
	  ages[index]= Double.parseDouble(st.nextToken(","));
	  temp=file.readLine();
	  maxFound=(totalAges>maxFound)?totalAges:maxFound;
	  if (totalAges == read_age_buckets) {
	    break;
	  }

	} while(!temp.equals(endTag));
	
	// read_age_buckets can be less than READ_AGE_BUCKETS
	// when totalAges is less, but it cannot be more than totalAges
	read_age_buckets = maxFound;
	
	for(int i=0; i<read_age_buckets; i++) {
	  sum += ages[i];
	  mutationHistory[i] += ages[i];
	}
	
	double cp = 0;
	// add positive dists to XYSeries
	for(int i=0; i<read_age_buckets; i++) {
	  if (ptrages[i] == null) {
	    ptrages[i]= new XYSeries("Bucket " + (i+1));
	  }
	  cp += (ages[i]/sum)*100;
	  if (!isTimeAllocs) {
	    ptrages[i].add(totalMutations/1000000, cp);
	  } else {
	    ptrages[i].add(totalAllocs, cp);
	  }
	}
	
	for(int i=0; i<read_age_buckets; i++) ages[i] = 0;
	totalAges = 0;
	sum = 0;
	// in case read_age_buckets is less than READ_AGE_BUCKETS
	read_age_buckets = READ_AGE_BUCKETS;
      }
    }
    
    double[] ageHist = new double[maxFound];
    // add XYSeries to the collection
    for(int i=0;i<read_age_buckets;i++) {
      String series = new String("Bucket " +(i+1));
      if(ptrages[i] == null) {
	continue;
      }
      ageHist[i] = (mutationHistory[i]/totalMutations)*100;
      dataset.addSeries(ptrages[i]);
    }
    return new double[][]{ageHist};
  }

  public static int getPosSeriesCount() {
    int pos = positiveSeriesCount;
    positiveSeriesCount = 0;
    return pos;
  }

  public static int getNegSeriesCount() {
    int neg = negativeSeriesCount;
    negativeSeriesCount = 0;
    return neg;
  }

  private static int getMaxCohorts(URL inURL)
    throws IOException {
    int maxCohort = 20000; // FIXME!!!
    String line = null;
    BufferedReader file = new BufferedReader(new InputStreamReader(inURL.openStream()));
      
      while((line=file.readLine()) != null) {
	if (line.equals("[max cohorts]")) {
	  maxCohort = Integer.parseInt(file.readLine());
	}
      }
      return maxCohort;
  }
}
