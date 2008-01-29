package dacapo.lusearch;

/**
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.File;
import java.lang.reflect.Constructor;

import dacapo.parser.Config;

/** Simple command-line based search demo. */
public class LusearchHarness extends dacapo.Benchmark {
  private final Object benchmark;
  
  public LusearchHarness(Config config, File scratch) throws Exception {
    super(config, scratch);
    Class<?> clazz = Class.forName("dacapo.lusearch.benchmark.LusearchBenchmark", true, loader);
    this.method = clazz.getMethod("main", String[].class);
    Constructor<?> cons = clazz.getConstructor();
    useBenchmarkClassLoader();
    try {
      benchmark = cons.newInstance();
    } finally {
      revertClassLoader();
    }
  }
  
  
  @Override
  public void iterate(String size) throws Exception {
    method.invoke(benchmark, (Object)preprocessArgs(size));
  }
}

