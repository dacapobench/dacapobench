package org.dacapo.harness;

import java.io.File;

import org.dacapo.harness.Benchmark;
import org.dacapo.parser.Config;

public class Chart extends Benchmark {

  public Chart(Config config, File scratch) throws Exception {
    super(config, scratch);
    Class<?> clazz = Class.forName("org.dacapo.chart.Plotter", true, loader);
    this.method = clazz.getMethod("main", String[].class);
  }

  public void iterate(String size) throws Exception {
    method.invoke(null, (Object) preprocessArgs(size));
  }
}
