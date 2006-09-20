The DaCapo Benchmark Suite
--------------------------

This benchmark suite is intend as a tool for the the memory management
research community.  It consists of a set of open source, real world
applications with non-trivial memory loads.

Guidelines for use
------------------

When quoting results in publications, the authors strongly request that
 - The exact version number of the suite be given
 - The suite be cited in accordance with the usual standards of acknowledging
   credit in academic research.

For more information see the Dacapo Benchmark web page, 
  http://www-ali.cs.umass.edu/DaCapo/gcbm.html


Building
--------

The easiest way to obtain the benchmark suite is to download the pre-built
jar file from the DaCapo Benchmark web site above.  If, however, you want 
to build from source read on:

The suite is built using ant, from a build.xml in this directory.  The most
useful targets are:

dist (default) - build the dacapo jar file from sources
sourcedist     - Download sources and build

Note the suite will currently only build using a Java 1.4 compatible SDK,
although the resulting jar file will happily run under the Sun 1.5 VM.

Some external tools are required to build the suite.  These include
 - ant      http://ant.apache.org/
 - javacc   http://javacc.dev.java.net/

Directories
-----------

src	Source files for the DaCapo test harness

stub	Stub files to allow the test harness to compile

sources	Source distributions for the packages used in
	the benchmark suite.  All files in this directory
	can be downloaded using the ant target 'sources'.

originals
	Source distributions for packages that are not available
	publicly (currently only 'ps').

patches	Some of the packages require minor changes to work in the
	benchmark suite.  These patches are applied to the source
	distributions before they are compiled.

cnf	Configuration files that tell the test harness how to run each
	of the benchmarks.

data	Test data


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

