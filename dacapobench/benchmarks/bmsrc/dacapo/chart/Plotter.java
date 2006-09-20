package dacapo.chart;
import java.io.File;
import java.net.URL;


public class Plotter implements GraphConstants{
  
  static String filePrefix;

  public static void main(String[] args) {
    String title = "";
    String outputDir = "";
    String prefix = "";
    String dataFile;
    int timeline = TIMELINE_MUTATIONS;
    boolean simple = false;
    
    for (int i = 0; i < (args.length - 1); i++) {
      if (args[i].equals("-d")) {
        // name of output directory
        outputDir = args[++i];
      } else if (args[i].equals("-p")) {
        // prefix
        prefix = args[++i];
      } else if (args[i].equals("small")) {
        simple = true;
      } else if (args[i].equals("large")) {
        simple = false;
      }
    }

    filePrefix = new String(outputDir + "/" + prefix + "-");
    URL in;
    try {
      in = (new File(args[args.length - 1])).toURL();

      // LARGE GRAPH
      // Perfect Line Graphs
      Graph graph1 = new Graph(in, PERFECT_PTR_TGT_XYPLOT, title, 
              timeline);
      Graph graph2 = new Graph(in, PERFECT_PTR_SRC_XYPLOT, title, 
              timeline);
      Graph graph3 = new Graph(in, PERFECT_PTR_DIST_XYPLOT, title, 
              timeline);
      Graph graph4 = new Graph(in, PERFECT_MUT_TGT_XYPLOT, title, 
              timeline);
      Graph graph5 = new Graph(in, PERFECT_MUT_SRC_XYPLOT, title, 
              timeline);
      Graph graph6 = new Graph(in, PERFECT_MUT_DIST_XYPLOT, title, 
              timeline);
      Graph graph7 = new Graph(in, PERFECT_HEAP_COMP_XYPLOT, title, 
              timeline);
      Graph graph8 = new Graph(in, PERFECT_INV_HEAP_COMP_XYPLOT, title, 
              timeline);
      // Cumulative Line Graphs (Imperfect)
      Graph graph9 = new Graph(in, CUMULATIVE_PTR_TGT_XYPLOT, title, 
              timeline);
      Graph graph10= new Graph(in, CUMULATIVE_PTR_SRC_XYPLOT, title, 
              timeline);
      Graph graph11= new Graph(in, CUMULATIVE_PTR_DIST_XYPLOT, title, 
              timeline);
      Graph graph12= new Graph(in, CUMULATIVE_MUT_TGT_XYPLOT, title, 
              timeline);
      Graph graph13= new Graph(in, CUMULATIVE_MUT_SRC_XYPLOT, title, 
              timeline);
      Graph graph14= new Graph(in, CUMULATIVE_MUT_DIST_XYPLOT, title, 
              timeline);
      if (simple) return;


      // Perfect Histograms
      Graph graph15 = new Graph(in, PERFECT_PTR_TGT_HIST, title);
      Graph graph16 = new Graph(in, PERFECT_PTR_SRC_HIST, title);
      Graph graph17 = new Graph(in, PERFECT_PTR_DIST_HIST, title);
      Graph graph18 = new Graph(in, PERFECT_MUT_TGT_HIST, title);
      Graph graph19 = new Graph(in, PERFECT_MUT_SRC_HIST, title);
      Graph graph20 = new Graph(in, PERFECT_MUT_DIST_HIST, title);
      //    Graph graph14 = new Graph(in, PERFECT_HEAP_COMP_HIST, title);
      //    Graph graph15 = new Graph(in, PERFECT_INV_HEAP_COMP_HIST, title);

      // Cumulative Histograms (Imperfect)
      Graph graph21 = new Graph(in, CUMULATIVE_PTR_TGT_HIST, title);
      Graph graph22 = new Graph(in, CUMULATIVE_PTR_SRC_HIST, title);
      Graph graph23 = new Graph(in, CUMULATIVE_PTR_DIST_HIST, title);
      Graph graph24 = new Graph(in, CUMULATIVE_MUT_TGT_HIST, title);
      Graph graph25 = new Graph(in, CUMULATIVE_MUT_SRC_HIST, title);
      Graph graph26 = new Graph(in, CUMULATIVE_MUT_DIST_HIST, title);

      // SMALL GRAPHS
      // Perfect Line Graphs
      Graph graph27 = new Graph(in, PERFECT_PTR_TGT_XYPLOT, true, title, 
              timeline);
      Graph graph28 = new Graph(in, PERFECT_PTR_SRC_XYPLOT, true, title, 
              timeline);
      Graph graph29 = new Graph(in, PERFECT_PTR_DIST_XYPLOT, true, title, 
              timeline);
      Graph graph30 = new Graph(in, PERFECT_MUT_TGT_XYPLOT, true, title, 
              timeline);
      Graph graph31 = new Graph(in, PERFECT_MUT_SRC_XYPLOT, true, title, 
              timeline);
      Graph graph32 = new Graph(in, PERFECT_MUT_DIST_XYPLOT, true, title, 
              timeline);
      Graph graph33 = new Graph(in, PERFECT_HEAP_COMP_XYPLOT, true, title, 
              timeline);
      Graph graph34 = new Graph(in, PERFECT_INV_HEAP_COMP_XYPLOT, true, 
              title, timeline);
      // Cumulative Line Graphs (Imperfect)
      Graph graph35 = new Graph(in, CUMULATIVE_PTR_TGT_XYPLOT, true, title,
              timeline);
      Graph graph36 = new Graph(in, CUMULATIVE_PTR_SRC_XYPLOT, true, title,
              timeline);
      Graph graph37 = new Graph(in, CUMULATIVE_PTR_DIST_XYPLOT, true,title,
              timeline);
      Graph graph38 = new Graph(in, CUMULATIVE_MUT_TGT_XYPLOT, true, title,
              timeline);
      Graph graph39 = new Graph(in, CUMULATIVE_MUT_SRC_XYPLOT, true, title,
              timeline);
      Graph graph40 = new Graph(in, CUMULATIVE_MUT_DIST_XYPLOT, true,title,
              timeline);

      // Perfect Histograms
      Graph graph41 = new Graph(in, PERFECT_PTR_TGT_HIST, true, title);
      Graph graph42 = new Graph(in, PERFECT_PTR_SRC_HIST, true, title);
      Graph graph43 = new Graph(in, PERFECT_PTR_DIST_HIST, true, title);
      Graph graph44 = new Graph(in, PERFECT_MUT_TGT_HIST, true, title);
      Graph graph45 = new Graph(in, PERFECT_MUT_SRC_HIST, true, title);
      Graph graph46 = new Graph(in, PERFECT_MUT_DIST_HIST, true, title);
//    Graph graph34 = new Graph(in, PERFECT_HEAP_COMP_HIST, true, title);
//    Graph graph35 = new Graph(in, PERFECT_INV_HEAP_COMP_HIST,true,title);

      // Cumulative Histograms (Imperfect)
      Graph graph47 = new Graph(in, CUMULATIVE_PTR_TGT_HIST, true, title);
      Graph graph48 = new Graph(in, CUMULATIVE_PTR_SRC_HIST, true, title);
      Graph graph49 = new Graph(in, CUMULATIVE_PTR_DIST_HIST, true, title);
      Graph graph50 = new Graph(in, CUMULATIVE_MUT_TGT_HIST, true, title);
      Graph graph51 = new Graph(in, CUMULATIVE_MUT_SRC_HIST, true, title);
      Graph graph52 = new Graph(in, CUMULATIVE_MUT_DIST_HIST, true, title);
    } catch (Exception e) {
      e.printStackTrace();
      return;
    }


  }

  private static void printUsage() {
      System.out.println("Usage: Plotter data_file [graph_title]\n");
      System.exit(1);
  }
}
