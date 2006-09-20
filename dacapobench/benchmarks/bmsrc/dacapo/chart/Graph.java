package dacapo.chart;

import org.jfree.chart.*;
import org.jfree.chart.axis.*;
import org.jfree.chart.plot.*;
import org.jfree.chart.renderer.*;
import org.jfree.chart.renderer.category.*;
import org.jfree.chart.renderer.xy.*;
import org.jfree.chart.title.*;
import org.jfree.data.*;
import org.jfree.data.category.*;
import org.jfree.data.general.*;
import org.jfree.data.xy.*;
import org.jfree.ui.*;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;


import java.awt.*;
import java.awt.geom.*;
import java.io.*;
import java.net.URL;

public class Graph implements GraphConstants {
  
  public Graph(URL fromURL, int chartType) {
    plotGraph(fromURL, DEFAULT_TO_FILE, "", chartType, TIMELINE_MUTATIONS,
              RANGE_IN_MB, DEFAULT_WIDTH, DEFAULT_HEIGHT, false);
  }

  public Graph(URL fromURL, int chartType, String extraTitle) {
    plotGraph(fromURL, DEFAULT_TO_FILE, extraTitle, chartType, 
	      TIMELINE_MUTATIONS, RANGE_IN_MB, DEFAULT_WIDTH, DEFAULT_HEIGHT,
	      false);
  }

  public Graph(URL fromURL,int chartType, String extraTitle,int timeLine) {
    plotGraph(fromURL, DEFAULT_TO_FILE, extraTitle, chartType, timeLine,
              RANGE_IN_MB, DEFAULT_WIDTH, DEFAULT_HEIGHT, false);
  }
  
  public Graph(URL fromURL, String toFile, int chartType){
    plotGraph(fromURL, toFile, "", chartType, TIMELINE_MUTATIONS,
              RANGE_IN_MB, DEFAULT_WIDTH, DEFAULT_HEIGHT, false);
  }
  
  public Graph(URL fromURL, int chartType, boolean isSmallGraph) {
    plotGraph(fromURL, DEFAULT_TO_FILE, "", chartType, TIMELINE_MUTATIONS,
              RANGE_IN_MB, DEFAULT_WIDTH, DEFAULT_HEIGHT, isSmallGraph);
  }
  
  public Graph(URL fromURL, int chartType, boolean isSmallGraph, 
	       String extraTitle) {
    plotGraph(fromURL, DEFAULT_TO_FILE, extraTitle, chartType, 
	      TIMELINE_MUTATIONS, RANGE_IN_MB, DEFAULT_WIDTH, DEFAULT_HEIGHT, 
	      isSmallGraph);
  }

  public Graph(URL fromURL, int chartType, boolean isSmallGraph, 
	       String extraTitle, int timeLine) {
    plotGraph(fromURL, DEFAULT_TO_FILE, extraTitle, chartType, timeLine,
              RANGE_IN_MB, DEFAULT_WIDTH, DEFAULT_HEIGHT, isSmallGraph);
  }
  
  public Graph(URL fromURL, String toFile, int chartType, 
	       boolean isSmallGraph) {
    plotGraph(fromURL, toFile, "", chartType, TIMELINE_MUTATIONS,
              RANGE_IN_MB, DEFAULT_WIDTH, DEFAULT_HEIGHT, isSmallGraph);
  }
  
  public Graph(URL fromURL, int chartType, int timeLine) {
    plotGraph(fromURL, DEFAULT_TO_FILE, "", chartType, timeLine, 
              RANGE_IN_MB, DEFAULT_WIDTH, DEFAULT_HEIGHT, false);
  }
  
  public Graph(URL fromURL, String toFile, int chartType, int timeLine) {
    plotGraph(fromURL, toFile, "", chartType, timeLine, RANGE_IN_MB, 
              DEFAULT_WIDTH, DEFAULT_HEIGHT, false);
  }
  
  public Graph(URL fromURL, int chartType, int timeLine, 
	       boolean isSmallGraph) {
    plotGraph(fromURL, DEFAULT_TO_FILE, "", chartType, timeLine, 
              RANGE_IN_MB, DEFAULT_WIDTH, DEFAULT_HEIGHT, isSmallGraph);
  }
  
  public Graph(URL fromURL, String toFile, int chartType, int timeLine, 
	       boolean isSmallGraph) {
    plotGraph(fromURL, toFile, "", chartType, timeLine, RANGE_IN_MB, 
              DEFAULT_WIDTH, DEFAULT_HEIGHT, isSmallGraph);
  }
  
  public Graph(URL fromURL, int chartType, int timeLine, int width, 
	       int height) {
    plotGraph(fromURL, DEFAULT_TO_FILE, "", chartType, timeLine, 
              RANGE_IN_MB, width, height, false);
  }
  
  private void plotGraph(URL fromURL, String toFile, String extraTitle, 
			 int chartType, int timeLine, 
			 int rangeType, int width, int height, 
			 boolean isSmallGraph) {
    
    CategoryDataset histData = null;
    XYSeriesCollection chartData = null;
    
    String chartTitle = null;
    String domainAxisLabel = null;
    String rangeAxisLabel = null;
    
    int graph_width = width;
    int graph_height = height;

    JFreeChart chart = null;
    
    OutputStream fileOut = null;
    Document pdfDoc;
    PdfContentByte cb = null;
    PdfTemplate tp = null;
    DefaultFontMapper mapper = new DefaultFontMapper();
    Graphics2D g2 = null;
    Rectangle2D r2D;

    PdfWriter writer = null;
    String fileName = toFile;
    
    int graphType = getGraphType(chartType);

    if (fileName.equals(DEFAULT_TO_FILE)) {
      fileName = getFilenameToSave(chartType);
    }
    
    if (extraTitle.equals("")) {
      chartTitle = new String(getChartTitleFromType(chartType));
    } else {
      chartTitle = new String(extraTitle);
    }
    
    try {
      if (isSmallGraph) fileName = fileName + "-sm";
      fileName = fileName + ".pdf";

      switch (graphType) {
      case HISTOGRAM_TYPE:
	if (isSmallGraph) {
	  graph_width = SMALL_HISTOGRAM_WIDTH; 
	  graph_height = SMALL_HISTOGRAM_HEIGHT;
	}
	fileOut = new BufferedOutputStream(new FileOutputStream(fileName));
	
	pdfDoc = new Document(new com.lowagie.text.Rectangle(graph_width, 
							     graph_height));
	writer = PdfWriter.getInstance(pdfDoc, fileOut);

	histData = Datasets.getHistDataset(fromURL, chartType, timeLine, 
					   rangeType); 

	domainAxisLabel = getHistogramDomainLabel(chartType);
	rangeAxisLabel = getRangeLabel(chartType);
	
	chart = ChartFactory.createStackedBarChart(chartTitle, domainAxisLabel,
						   rangeAxisLabel, histData,
						   PlotOrientation.VERTICAL,
						   false, true, false);
	
	CategoryPlot catPlot = chart.getCategoryPlot();
	makeReadable(catPlot, !isSmallGraph, chartType);
        //	makeReadable(catPlot, true, chartType);
	
	if (isSmallGraph) {
	  chart.getTitle().setFont(new java.awt.Font("SansSerif", 
						     java.awt.Font.PLAIN, 10));
	}
	
	chart.setBorderVisible(false);
	chart.setBackgroundPaint(Color.white);

	pdfDoc.open();

        cb = writer.getDirectContent();
        tp = cb.createTemplate(graph_width, graph_height);
        g2 = tp.createGraphics(graph_width,graph_height,mapper);

	r2D = new Rectangle2D.Double(0, 0, graph_width, graph_height);
        chart.draw(g2,r2D,null);
        g2.dispose();
        cb.addTemplate(tp,0,0);
	
	pdfDoc.close();
	break;
      case LINEGRAPH_TYPE:
	if (isSmallGraph) {
	  graph_width = SMALL_WIDTH; 
	  graph_height = SMALL_HEIGHT;

	  if (chartType == PERFECT_HEAP_COMP_XYPLOT) {
	    graph_height += 10;
	  }
	}
	fileOut = new BufferedOutputStream(new FileOutputStream(fileName));

	pdfDoc = new Document(new com.lowagie.text.Rectangle(graph_width, 
							     graph_height));
	writer = PdfWriter.getInstance(pdfDoc, fileOut);
	
	if (chartType == PERFECT_INV_HEAP_COMP_XYPLOT) {
	  rangeType = RANGE_IN_KB;
	}

	chartData = Datasets.getLineDataset(fromURL, chartType, 
					    timeLine, rangeType); 
	
	domainAxisLabel = getLineGraphDomainLabel(timeLine);
	rangeAxisLabel = getRangeLabel(chartType);
	
	chart = ChartFactory.createXYLineChart(chartTitle, domainAxisLabel,
					       rangeAxisLabel, chartData,
					       PlotOrientation.VERTICAL,
					       false, true, false);
	
	XYPlot xyPlot = chart.getXYPlot();
	makeReadable(xyPlot, !isSmallGraph, chartType);

	if (isSmallGraph) {
	  chart.getTitle().setFont(new java.awt.Font("SansSerif", 
						     java.awt.Font.PLAIN, 10));
	}

	chart.setBorderVisible(false);
	chart.setBackgroundPaint(Color.white);
	chart.getTitle().setHorizontalAlignment(HorizontalAlignment.CENTER);
	chart.getTitle().setPosition(RectangleEdge.BOTTOM);

	pdfDoc.open();

	cb = writer.getDirectContent();
	tp = cb.createTemplate(graph_width, graph_height);
	g2 = tp.createGraphics(graph_width, graph_height, mapper);
	
	r2D = new Rectangle2D.Double(0, 0, graph_width, graph_height);
	chart.draw(g2, r2D, null);
	g2.dispose();
	cb.addTemplate(tp, 0, 0);

	pdfDoc.close();
	break;
      default: System.out.println("plotGraph: No such graph type"); break;
      }
      
      System.out.println("Done plotting: " + fileName);
    }
    catch(Exception ex) { System.out.println("Exception " + ex); ex.printStackTrace(); }
  }
  
  public String getChartTitleFromType(int chartType) {
    String title = "Graph";
    
    switch (chartType) {
      // Histograms
    case PERFECT_PTR_DIST_HIST:
      title = "Pointer Distance (\"perfect\" heap)"; break;
    case PERFECT_PTR_SRC_HIST:
      title = "Pointer Source (\"perfect\" heap)"; break;
    case PERFECT_PTR_TGT_HIST:
      title = "Pointer Target (\"perfect\" heap)"; break;
    case PERFECT_MUT_DIST_HIST:
      title = "Mutation Distance (\"perfect\" heap)"; break;
    case PERFECT_MUT_SRC_HIST:
      title = "Mutation Source (\"perfect\" heap)"; break;
    case PERFECT_MUT_TGT_HIST:
      title = "Mutation Target (\"perfect\" heap)"; break;
    case PERFECT_HEAP_COMP_HIST:
      title = "Heap Composition"; break;
    case PERFECT_INV_HEAP_COMP_HIST:
      title = "Inverted Heap Composition"; break;
    case CUMULATIVE_PTR_DIST_HIST:
      title = "Pointer Distance"; break;
    case CUMULATIVE_PTR_SRC_HIST:
      title = "Pointer Source"; break;
    case CUMULATIVE_PTR_TGT_HIST:
      title = "Pointer Target"; break;
    case CUMULATIVE_MUT_DIST_HIST:
      title = "Mutation Distance"; break;
    case CUMULATIVE_MUT_SRC_HIST:
      title = "Mutation Source"; break;
    case CUMULATIVE_MUT_TGT_HIST:
      title = "Mutation Target"; break;
      
      // XY-Line graphs
    case PERFECT_PTR_DIST_XYPLOT:
      title = "Pointer Distance Time-series (\"perfect\" heap)"; break;
    case PERFECT_PTR_SRC_XYPLOT:
      title = "Pointer Source Time-series (\"perfect\" heap)"; break;
    case PERFECT_PTR_TGT_XYPLOT:
      title = "Pointer Target Time-series (\"perfect\" heap)"; break;
    case PERFECT_MUT_DIST_XYPLOT:
      title = "Mutation Distance Time-series (\"perfect\" heap)"; break;
    case PERFECT_MUT_SRC_XYPLOT:
      title = "Mutation Source Time-series (\"perfect\" heap)"; break;
    case PERFECT_MUT_TGT_XYPLOT:
      title = "Mutation Target Time-series (\"perfect\" heap)"; break;
    case PERFECT_HEAP_COMP_XYPLOT:
      title = "Heap Composition Time-series"; break;
    case PERFECT_INV_HEAP_COMP_XYPLOT:
      title = "Inverted Heap Composition Time-series"; break;
    case CUMULATIVE_PTR_DIST_XYPLOT:
      title = "Pointer Distance Time-series"; break;
    case CUMULATIVE_PTR_SRC_XYPLOT:
      title = "Pointer Source Time-series"; break;
    case CUMULATIVE_PTR_TGT_XYPLOT:
      title = "Pointer Target Time-series"; break;
    case CUMULATIVE_MUT_DIST_XYPLOT:
      title = "Mutation Distance Time-series"; break;
    case CUMULATIVE_MUT_SRC_XYPLOT:
      title = "Mutation Source Time-series"; break;
    case CUMULATIVE_MUT_TGT_XYPLOT:
      title = "Mutation Target Time-series"; break;
    default: 
      System.out.println("getChartTitleFromType: No such graph"); break;
    }     
    return title;
  }
  
  
  public String getHistogramDomainLabel(int chartType) {
    String label = "X";
    
    switch (chartType) {
      // Histograms
    case PERFECT_PTR_DIST_HIST:
    case PERFECT_PTR_SRC_HIST:
    case PERFECT_PTR_TGT_HIST:
    case CUMULATIVE_PTR_DIST_HIST:
    case CUMULATIVE_PTR_SRC_HIST:
    case CUMULATIVE_PTR_TGT_HIST:
      label = "Log_2 of Pointer Distances (Bytes)"; break;
    case PERFECT_MUT_DIST_HIST:
    case PERFECT_MUT_SRC_HIST:
    case PERFECT_MUT_TGT_HIST:
    case CUMULATIVE_MUT_DIST_HIST:
    case CUMULATIVE_MUT_SRC_HIST:
    case CUMULATIVE_MUT_TGT_HIST:
      label = "Log_2 of Mutation Distances (Bytes)"; break;
    case PERFECT_HEAP_COMP_HIST:
    case PERFECT_INV_HEAP_COMP_HIST:
      label = "Cohorts"; break;
    default: 
      System.out.println("getHistogramDomainLabel: No such graph"); break;
    }     
    return label;
  }
  
  public String getLineGraphDomainLabel(int timeLine) {
    String label = "X";
    
    switch(timeLine) {
    case TIMELINE_MUTATIONS:
      label = "Mutations (in millions)"; break;
    case TIMELINE_ALLOCATIONS:
      label = "Allocations (KB)"; break;
    default: 
      System.out.println("getLineGraphDomainLabel: No such timeline");
      break;
    }
    return label;
  }
  
  public String getRangeLabel(int chartType) {
    String label = "X";
    
    switch(chartType) {
      // Histograms
    case PERFECT_PTR_DIST_HIST:
    case PERFECT_PTR_SRC_HIST:
    case PERFECT_PTR_TGT_HIST:
    case CUMULATIVE_PTR_DIST_HIST:
    case CUMULATIVE_PTR_SRC_HIST:
    case CUMULATIVE_PTR_TGT_HIST:
    case PERFECT_MUT_DIST_HIST:
    case PERFECT_MUT_SRC_HIST:
    case PERFECT_MUT_TGT_HIST:
    case CUMULATIVE_MUT_DIST_HIST:
    case CUMULATIVE_MUT_SRC_HIST:
    case CUMULATIVE_MUT_TGT_HIST:
      label = "Pointer Mutations (%)"; break;
    case PERFECT_HEAP_COMP_HIST:
    case PERFECT_INV_HEAP_COMP_HIST:
      label = "Volume (MB)"; break;
      
      // XY-Line graphs
    case PERFECT_PTR_DIST_XYPLOT:
    case PERFECT_PTR_SRC_XYPLOT:
    case PERFECT_PTR_TGT_XYPLOT:
    case CUMULATIVE_PTR_DIST_XYPLOT:
    case CUMULATIVE_PTR_SRC_XYPLOT:
    case CUMULATIVE_PTR_TGT_XYPLOT:
    case PERFECT_MUT_DIST_XYPLOT:
    case PERFECT_MUT_SRC_XYPLOT:
    case PERFECT_MUT_TGT_XYPLOT:
    case CUMULATIVE_MUT_DIST_XYPLOT:
    case CUMULATIVE_MUT_SRC_XYPLOT:
    case CUMULATIVE_MUT_TGT_XYPLOT:
      label = "Distances (%)"; break;
    case PERFECT_HEAP_COMP_XYPLOT:
      label = "Cohort Volume (MB)"; break;
    case PERFECT_INV_HEAP_COMP_XYPLOT:
      label = "Cohort Volume (KB)"; break;
    default: System.out.println("getRangeLabel: No such range type"); break;
    }
    return label;
  }
  public String getFilenameToSave(int chartType) {
    String filename = "graph";
    
    switch (chartType) {
      // Histograms
    case PERFECT_PTR_DIST_HIST:
      filename = "dst-ptr-prf-hist"; break;
    case PERFECT_PTR_SRC_HIST:
      filename = "src-ptr-prf-hist"; break;
    case PERFECT_PTR_TGT_HIST:
      filename = "tgt-ptr-prf-hist"; break;

    case PERFECT_MUT_DIST_HIST:
      filename = "dst-mut-prf-hist"; break;
    case PERFECT_MUT_SRC_HIST:
      filename = "src-mut-prf-hist"; break;
    case PERFECT_MUT_TGT_HIST:
      filename = "tgt-mut-prf-hist"; break;

    case CUMULATIVE_PTR_DIST_HIST:
      filename = "dst-ptr-inf-hist"; break;
    case CUMULATIVE_PTR_SRC_HIST:
      filename = "src-ptr-inf-hist"; break;
    case CUMULATIVE_PTR_TGT_HIST:
      filename = "tgt-ptr-inf-hist"; break;

    case CUMULATIVE_MUT_DIST_HIST:
      filename = "dst-mut-inf-hist"; break;
    case CUMULATIVE_MUT_SRC_HIST:
      filename = "src-mut-inf-hist"; break;
    case CUMULATIVE_MUT_TGT_HIST:
      filename = "tgt-mut-inf-hist"; break;

      // XY-Line graphs
    case PERFECT_PTR_DIST_XYPLOT:
      filename = "dst-ptr-prf-time"; break;
    case PERFECT_PTR_SRC_XYPLOT:
      filename = "src-ptr-prf-time"; break;
    case PERFECT_PTR_TGT_XYPLOT:
      filename = "tgt-ptr-prf-time"; break;

    case PERFECT_MUT_DIST_XYPLOT:
      filename = "dst-mut-prf-time"; break;
    case PERFECT_MUT_SRC_XYPLOT:
      filename = "src-mut-prf-time"; break;
    case PERFECT_MUT_TGT_XYPLOT:
      filename = "tgt-mut-prf-time"; break;

    case CUMULATIVE_PTR_DIST_XYPLOT:
      filename = "dst-ptr-inf-time"; break;
    case CUMULATIVE_PTR_SRC_XYPLOT:
      filename = "src-ptr-inf-time"; break;
    case CUMULATIVE_PTR_TGT_XYPLOT:
      filename = "tgt-ptr-inf-time"; break;

    case CUMULATIVE_MUT_DIST_XYPLOT:
      filename = "dst-mut-inf-time"; break;
    case CUMULATIVE_MUT_SRC_XYPLOT:
      filename = "src-mut-inf-time"; break;
    case CUMULATIVE_MUT_TGT_XYPLOT:
      filename = "tgt-mut-inf-time"; break;

    case PERFECT_HEAP_COMP_XYPLOT:
      filename = "heap-comp-b"; break;
    case PERFECT_INV_HEAP_COMP_XYPLOT:
      filename = "heap-comp-a"; break;
    default: 
	  System.out.println("getFilenameToSave: No such chart type"); break;
    }     

    return new String(Plotter.filePrefix + filename);
  }
  
  private int getGraphType(int chartType) {
    // Add new chart types under the correct graph type

    switch (chartType) {
      // Histograms
    case PERFECT_PTR_DIST_HIST:
    case PERFECT_PTR_SRC_HIST:
    case PERFECT_PTR_TGT_HIST:
    case PERFECT_MUT_DIST_HIST:
    case PERFECT_MUT_SRC_HIST:
    case PERFECT_MUT_TGT_HIST:
    case PERFECT_HEAP_COMP_HIST:
    case PERFECT_INV_HEAP_COMP_HIST:
    case CUMULATIVE_PTR_DIST_HIST:
    case CUMULATIVE_PTR_SRC_HIST:
    case CUMULATIVE_PTR_TGT_HIST:
    case CUMULATIVE_MUT_DIST_HIST:
    case CUMULATIVE_MUT_SRC_HIST:
    case CUMULATIVE_MUT_TGT_HIST:
      return HISTOGRAM_TYPE;
      
      // XY-Line graphs
    case PERFECT_PTR_DIST_XYPLOT:
    case PERFECT_PTR_SRC_XYPLOT:
    case PERFECT_PTR_TGT_XYPLOT:
    case PERFECT_MUT_DIST_XYPLOT:
    case PERFECT_MUT_SRC_XYPLOT:
    case PERFECT_MUT_TGT_XYPLOT:
    case PERFECT_HEAP_COMP_XYPLOT:
    case PERFECT_INV_HEAP_COMP_XYPLOT:
    case CUMULATIVE_PTR_DIST_XYPLOT:
    case CUMULATIVE_PTR_SRC_XYPLOT:
    case CUMULATIVE_PTR_TGT_XYPLOT:
    case CUMULATIVE_MUT_DIST_XYPLOT:
    case CUMULATIVE_MUT_SRC_XYPLOT:
    case CUMULATIVE_MUT_TGT_XYPLOT:
      return LINEGRAPH_TYPE;
    default: 
      System.out.println("getGraphType: No such chart type"); break;
    }
    return -1;
  }
  
  private void makeReadable(CategoryPlot plot, boolean isAxisVisible, 
			    int chartType) {
    
    Color lightColor = Color.lightGray;
    Color darkColor = Color.black;
    
    BarChartRenderer renderer = new BarChartRenderer();
    plot.setRenderer(renderer);
    plot.getDomainAxis().setVisible(isAxisVisible);
    plot.getRangeAxis().setVisible(isAxisVisible);

    // Set Fonts
    plot.getDomainAxis().setLabelFont(new java.awt.Font("SansSerif", 
						      java.awt.Font.PLAIN,10));
    plot.getRangeAxis().setLabelFont(new java.awt.Font("SansSerif", 
						      java.awt.Font.PLAIN,10));

 //   if (chartType == PERFECT_HEAP_COMP_HIST 
 //	|| chartType == PERFECT_INV_HEAP_COMP_HIST) {
 //      plot.getDomainAxis().setSkipCategoryLabelsToFit(true);
 //    }
  }
  
  private void makeReadable(XYPlot plot,boolean isDomainVisible,int chartType){
    
    int posSeriesCount = 0;
    int negSeriesCount = 0;
    
    BasicStroke light = new BasicStroke(0.8f);
    Color lightColor = Color.lightGray;
    
    BasicStroke dark = new BasicStroke(1f);
    Color darkColor = Color.black;
    XYItemRenderer renderer = plot.getRenderer();
    
    int counter = 0;
    switch (chartType) {
      // XY-Line graphs
    case PERFECT_PTR_DIST_XYPLOT:
    case CUMULATIVE_PTR_DIST_XYPLOT:
    case PERFECT_MUT_DIST_XYPLOT:
    case CUMULATIVE_MUT_DIST_XYPLOT:
      posSeriesCount = Datasets.getPosSeriesCount();
      negSeriesCount = Datasets.getNegSeriesCount();

      for(int i=0; i<posSeriesCount; i++){
	if(i%5==0) {
	  renderer.setSeriesStroke(i, dark);
	  renderer.setSeriesPaint(i, darkColor);
	} else {
	  renderer.setSeriesStroke(i, light);
	  renderer.setSeriesPaint(i, lightColor);
	}
      }

      for(int i=posSeriesCount; i<=plot.getSeriesCount(); i++){
	if(counter++ == 5) {
	  renderer.setSeriesStroke(i, dark);
	  renderer.setSeriesPaint(i, darkColor);
	  counter = 1;
	} else {
	  renderer.setSeriesStroke(i, light);
	  renderer.setSeriesPaint(i, lightColor);
	}
      }
      renderer.setSeriesStroke(posSeriesCount, dark);
      renderer.setSeriesPaint(posSeriesCount, darkColor);
      break;
    case PERFECT_PTR_SRC_XYPLOT:
    case PERFECT_PTR_TGT_XYPLOT:
    case CUMULATIVE_PTR_SRC_XYPLOT:
    case CUMULATIVE_PTR_TGT_XYPLOT:
    case PERFECT_MUT_SRC_XYPLOT:
    case PERFECT_MUT_TGT_XYPLOT:
    case CUMULATIVE_MUT_SRC_XYPLOT:
    case CUMULATIVE_MUT_TGT_XYPLOT:
    case PERFECT_HEAP_COMP_XYPLOT:
    case PERFECT_INV_HEAP_COMP_XYPLOT:
      for(int i=0; i<plot.getSeriesCount(); i++){
	if(i%5==0) {
	  renderer.setSeriesStroke(i, dark);
	  renderer.setSeriesPaint(i, darkColor);
	} else {
	  renderer.setSeriesStroke(i, light);
	  renderer.setSeriesPaint(i, lightColor);
	}
      }
      break;
    }
    plot.getDomainAxis().setVisible(isDomainVisible);

    // Set Fonts
    plot.getDomainAxis().setLabelFont(new java.awt.Font("SansSerif", 
						      java.awt.Font.PLAIN,10));
    plot.getRangeAxis().setLabelFont(new java.awt.Font("SansSerif", 
						      java.awt.Font.PLAIN,10));
    
    if ((chartType == PERFECT_HEAP_COMP_XYPLOT) && !isDomainVisible) {
      plot.setDomainAxisLocation(AxisLocation.TOP_OR_RIGHT);      
      plot.getDomainAxis().setVisible(true);
    }
  }
}

class BarChartRenderer extends StackedBarRenderer {
  
  private Color darkColor = Color.black;
  private Color lightColor = Color.lightGray;
  
  public Paint getItemPaint(int row, int column) {
    if (column%5==0) {
      setPaint(darkColor);
    } else {
      setPaint(lightColor);
    }
    return super.getItemPaint(row, column);
  }
}
