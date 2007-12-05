package dacapo.chart;

import java.io.File;

import dacapo.Benchmark;
import dacapo.parser.Config;


public class ChartHarness extends Benchmark {

  public ChartHarness(Config config, File scratch) throws Exception {
    super(config, scratch);
    Class<?> clazz = Class.forName("dacapo.chart.plotter.Plotter", true, loader);
    this.method = clazz.getMethod("main", String[].class );
  }

  public void iterate(String size) throws Exception {
    ClassLoader dacapoCL = Thread.currentThread().getContextClassLoader();
    Thread.currentThread().setContextClassLoader(loader);
    method.invoke(null, (Object) preprocessArgs(size));
    Thread.currentThread().setContextClassLoader(dacapoCL);
  }

}
