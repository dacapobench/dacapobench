The DaCapo Benchmark Suite
--------------------------

Last updated 2009-06-26

This benchmark suite is intend as a tool for the research community.  It
consists of a set of open source, real world applications with non-trivial
memory loads.

Guidelines for use
------------------

When quoting results in publications, the authors strongly request that
 - The exact version number of the suite be given
 - The suite be cited in accordance with the usual standards of acknowledging
   credit in academic research.

For more information see the Dacapo Benchmark web page, 
  http://dacapobench.org


Building
--------

The easiest way to obtain the benchmark suite is to download the pre-built
jar file from the DaCapo Benchmark web site above.

If, however, you want to build from source read on...

The suite is built using ant.  You will need the following tools:
	- ant 		(you need to install this yourself if you don't already have it http://ant.apache.org)
	- javacc	(included in our tools directory, or download at http://javacc.dev.java.net/)
	- maven		(included in our tools directory, or download at http://maven.apache.org/download.html)


IMPORTANT: before trying to build the suite:

	1. Set your JAVA_HOME environment variable appropriately
	
	2. Copy default.properties to local.properties and edit it
	   for your environment.
	   
	3. Ensure maven (mvn) is in your execution path.  You can use
	   the version that we include in our tools directory if you wish.
	   We have had some problems with older versions of maven.

	
For more information, invoke ant with the help target in the benchmarks
directory ("ant help").


Structure:  Files and Directories
-----------

harness	The benchmark harness

	This directory includes all of the source code for the DaCapo harness,
	which is used to invoke the benchmarks, validate output, etc.
	
	
bms		The benchmarks

	bms/<bm>/src		Source written by the DaCapo group to drive the benchmark <bm>
	bms/<bm>/downloads	MD5 sums for each of the requisite downloads.  These are used to
						cache the downloads (avoiding re-downloading on each build)
	bms/<bm>/data		Directory containing any data used to drive the benchmark
	bms/<bm>/<bm>.cnf	Configuration file for <bm>
	bms/<bm>/<bm>.patch	Patches against the orginal sources (if any)
	bms/<bm>/build.xml	Local build file for <bm>
	bms/<bm>/build		[Directory where building occurs.  This is only created at build time]
	bms/<bm>/dist		[Directory where the result of the build goes.  This is only created at build time]


libs	Common code used by one or more benchmarks.

	Each of these directories more or less mirror the bm directories.



License
-------

The DaCapo Benchmark Suite conmprises several open source or public domain
programs, plus a test harness, some patches to enable the benchmarks to
run under the test harness, and a packaging process.  The benchmarks are
distributed under their own licenses and the remaining component is 
distributed under the Apache License, version 2.0.

   Copyright 2005 The DaCapo Project,
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

