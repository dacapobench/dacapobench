package dacapo.chart;

public interface GraphConstants {
  
  // Graph Types
  static final int HISTOGRAM_TYPE                = 40;
  static final int LINEGRAPH_TYPE                = 50;
  
  // Chart Types: (Histograms)
  static final int PERFECT_PTR_DIST_HIST         = 1;
  static final int PERFECT_PTR_SRC_HIST          = 2;
  static final int PERFECT_PTR_TGT_HIST          = 3;
  static final int PERFECT_MUT_DIST_HIST         = 4;
  static final int PERFECT_MUT_SRC_HIST          = 5;
  static final int PERFECT_MUT_TGT_HIST          = 6;
  static final int PERFECT_HEAP_COMP_HIST        = 7;
  static final int PERFECT_INV_HEAP_COMP_HIST    = 8;
  
  static final int CUMULATIVE_PTR_DIST_HIST      = 9;
  static final int CUMULATIVE_PTR_SRC_HIST       = 10;
  static final int CUMULATIVE_PTR_TGT_HIST       = 11;
  static final int CUMULATIVE_MUT_DIST_HIST      = 12;
  static final int CUMULATIVE_MUT_SRC_HIST       = 13;
  static final int CUMULATIVE_MUT_TGT_HIST       = 14;
  
  // Chart Types: (Line-graphs)
  static final int PERFECT_PTR_DIST_XYPLOT       = 15;
  static final int PERFECT_PTR_SRC_XYPLOT        = 16;
  static final int PERFECT_PTR_TGT_XYPLOT        = 17;
  static final int PERFECT_MUT_DIST_XYPLOT       = 18;
  static final int PERFECT_MUT_SRC_XYPLOT        = 19;
  static final int PERFECT_MUT_TGT_XYPLOT        = 20;
  static final int PERFECT_HEAP_COMP_XYPLOT      = 21;
  static final int PERFECT_INV_HEAP_COMP_XYPLOT  = 22;
  
  static final int CUMULATIVE_PTR_DIST_XYPLOT    = 23;
  static final int CUMULATIVE_PTR_SRC_XYPLOT     = 24;
  static final int CUMULATIVE_PTR_TGT_XYPLOT     = 25;
  static final int CUMULATIVE_MUT_DIST_XYPLOT    = 26;
  static final int CUMULATIVE_MUT_SRC_XYPLOT     = 27;
  static final int CUMULATIVE_MUT_TGT_XYPLOT     = 28;
  
  // Timelines (x-axis)
  static final int TIMELINE_MUTATIONS            = 29;
  static final int TIMELINE_ALLOCATIONS          = 30;
  
  // Range axis (y-axis)
  static final int RANGE_IN_KB                   = 31;
  static final int RANGE_IN_MB                   = 32;
  
  // Graph sizes
  static final int DEFAULT_WIDTH                 = 640;
  static final int DEFAULT_HEIGHT                = 480;
  
//   static final int SMALL_WIDTH                   = 250;
//   static final int SMALL_HEIGHT                  = 200;
  static final int SMALL_WIDTH                   = 450;
  static final int SMALL_HEIGHT                  = 200;
  
  static final int SMALL_HISTOGRAM_WIDTH         = SMALL_HEIGHT;
  static final int SMALL_HISTOGRAM_HEIGHT        = SMALL_HEIGHT;
  
  // Save file as
  static final String DEFAULT_TO_FILE            = "graph.jpeg";
}
