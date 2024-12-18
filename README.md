# The DaCapo Benchmark Suite

Last updated 2023-11-08

This benchmark suite is intend as a tool for the research community.
It consists of a set of open source, real world applications with
non-trivial memory loads.


## Guidelines for use

When quoting results in publications, the authors of this suite
strongly request that:

* The exact version of the suite be given (number & name, eg 'dacapo-23.11-chopin')

* The suite be cited in accordance with the usual standards of acknowledging credit in academic research.

* Please cite the [2006 OOPSLA paper](http://doi.acm.org/10.1145/1167473.1167488) or a more recent paper by the DaCapo authors providing an up-to-date description of the suite if and when such a paper becomes available.

* All command line options used be reported.  For example, if you explicitly override the number of threads or set the number of iterations, you must report this when you publish results.  Likewise you should report exactly which JVM version you use, and all commandline options provided to the JVM.

For more information see the [DaCapo Benchmark web page](http://dacapobench.org).


## Building

The easiest way to obtain the benchmark suite is to download the pre-built suite file from the DaCapo Benchmark web site above.

If, however, you want to build from source read on...

##### Run ant:

`ant -p`      [prints out description, including configuration and environment variable settings]

`ant`         [builds all benchmarks, creates a zip file]

`ant dist`    [builds all benchmarks, this is the default]

`ant source`  [builds a source distribution including benchmarks and tools]

`ant bm`      [builds a specific benchmark, bm]

**NOTE**

A log of each directory is created under this benchmark directory
for benchmark build status and build success or failure files
to be stored.  The directory log directory is normally of the
form
`${basedir}/log/${build.time}`
and contains status.txt where each benchmark build status is recorded,
and either pass.txt if all benchmarks build, or fail.txt if one or
more benchmarks fail to build. Note: that either fail.txt or pass.txt
is created when a full build is performed.

**IMPORTANT:** before trying to build the suite:

1. Set your `JAVA_HOME` environment variable appropriately (it must be set and be consistent with the VM that will be used to build the suite).

2. Create the `local.properties` file (using `default.properties` as a template)
   
3. Set `jdk.11.home`, in the `local.properties`, to point to a Java 11 installation.


For more information, run `ant -p` in the benchmarks directory.

## Customization

It is possible to use callbacks to run code before a benchmark starts, when it stops, and when the run has completed.
To do so, extend the class `Callback` (see the file [`harness/src/ExampleCallback.java`](https://github.com/dacapobench/dacapobench/blob/main/benchmarks/harness/src/ExampleCallback.java) for an example).

To run a benchmark with your callback, run:

    java -jar dacapo-23.11-chopin.jar -c <callback> <benchmark>

## Source Code Structure

### `harness` (The benchmark harness)

This directory includes all of the source code for the DaCapo harness, which is used to invoke the benchmarks, validate output, etc.

	
### `bms` (The benchmarks)

* `bms/<bm>/src` Source written by the DaCapo group to drive the benchmark `<bm>`
* `bms/<bm>/downloads`	MD5 sums for each of the requisite downloads.  These are used to cache the downloads (avoiding re-downloading on each build)
* `bms/<bm>/data` Directory containing any data used to drive the benchmark
* `bms/<bm>/<bm>.cnf`	Configuration file for `<bm>`
* `bms/<bm>/<bm>.patch`	Patches against the orginal sources (if any)
* `bms/<bm>/stats-*.yml`	Workload statistics for `<bm>`
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

   Copyright 2023 The DaCapo Project,
   Schoool of Computer Sciences
   Australian National University
   Acton, ACT 2601
   Australia

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
