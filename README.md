# The DaCapo Benchmark Suite

Last updated 2009-12-18

This benchmark suite is intend as a tool for the research community.
It consists of a set of open source, real world applications with
non-trivial memory loads.


## Guidelines for use

When quoting results in publications, the authors of this suite
strongly request that:

* The exact version of the suite be given (number & name)

* The suite be cited in accordance with the usual standards of acknowledging credit in academic research.

* Please cite the [2006 OOPSLA paper](http://doi.acm.org/10.1145/1167473.1167488)

* All command line options used be reported.  For example, if you explicitly override the number of threads or set the number of iterations, you must report this when you publish results. 

For more information see the [DaCapo Benchmark web page](http://dacapobench.org).


## Building

The easiest way to obtain the benchmark suite is to download the pre-built jar file from the DaCapo Benchmark web site above.

If, however, you want to build from source read on...

The suite is built using ant 1.9 (1.10 and later will fail for many of the benchmarks).  You will need the following tools:

* *[ant 1.9](http://ant.apache.org)* You need to install this yourself if you don't already have it.

* *[javacc](http://javacc.dev.java.net/)* Included in our tools directory.

* *[maven](http://maven.apache.org)* Included in our tools directory.

* *[cvs](http:/www.nongnu.org/cvs)

* *[svn](http://subversion.apache.org)


**NOTE**

1. A number of benchmarks including trade and tomcat do not reliably _build_ under Java 6 (all run under Java 6). Therefore you must explicitly use a Java 5 VM at build time (see below).
	

**IMPORTANT:** before trying to build the suite:

1. Set your `JAVA_HOME` environment variable appropriately (it must be set and be consistent with the VM that will be used to build the suite).

2. Copy `default.properties` to `local.properties` and edit it for your environment.

  * Specifically, you must set `java14.lib` to point to a Java 1.4 installation and `java14.compile.classpath` to correctly capture the libraries for that installation(otherwise derby will not build correctly)


For more information, run `ant -p` in the benchmarks directory.



## Source Code Structure

### `harness` (The benchmark harness)

This directory includes all of the source code for the DaCapo harness, which is used to invoke the benchmarks, validate output, etc.
	
	
### `bms` (The benchmarks)

* `bms/<bm>/src` Source written by the DaCapo group to drive the benchmark `<bm>`
* `bms/<bm>/downloads`	MD5 sums for each of the requisite downloads.  These are used to cache the downloads (avoiding re-downloading on each build)
* `bms/<bm>/data` Directory containing any data used to drive the benchmark
* `bms/<bm>/<bm>.cnf`	Configuration file for `<bm>`
* `bms/<bm>/<bm>.patch`	Patches against the orginal sources (if any)
* `bms/<bm>/build.xml`	Local build file for <bm>
* `bms/<bm>/build` _Directory where building occurs.  This is only created at build time._
* `bms/<bm>/dist` _Directory where the result of the build goes.  This is only created at build time._


### `libs` (Common code used by one or more benchmarks.)

Each of these directories more or less mirror the `bm` directories.



## License

The DaCapo Benchmark Suite conmprises several open source or public
domain programs, plus a test harness, some patches to enable the
benchmarks to run under the test harness, and a packaging process. The
benchmarks are distributed under their own licenses and the remaining
component is distributed under the Apache License, version 2.0.

   Copyright 2009 The DaCapo Project,
   Department of Computer Science
   University of Massachusetts,
   Amherst MA. 01003, USA

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
