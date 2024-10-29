# dacapo-23.11-MR1-chopin RELEASE NOTES 2024-10

This is a maintence release for 23.11, the third major release of the DaCapo benchmark suite.

*We strongly welcome feedback, bug fixes and suggestions.   Please do this via [github](https://github.com/dacapobench/dacapobench)*

These notes are structured as follows:

1. Overview
2. Usage
3. Changes
4. Known problems and limitations
5. Contributions and Acknowledgements

# 1. Overview

The 23.11-MR1 release is a maintence release of 23.11.  It does not change the set of benchmarks or their versions, compared to 23.11.  It includes several bug fixes and a number of new features enhancing the usability of the suite (details below).

The 23.11 release was the third major update of the suite.  It is incompatible with previous releases: new benchmarks have been added, old benchmarks have been removed, all other benchmarks have been substantially updated and the inputs have changed for every program. It is for this reason that **in any published use of the suite, the version of the suite must be explicitly stated**.

The 23.11 release included: new packaging as a zip file containg a jar plus a read-only folder structure; the addition of eight completely new benchmarks: biojava, cassandra, graphchi, h2o, jme, kafka, spring, and zxing; the inclusion of six benchmarks with large (> 1 GB) heap footprints; the inclusion of nine latency-sensitive benchmarks; the move from Geronimo to JBoss Wildfly for the trade benchmarks; and the upgrade of all other benchmarks to reflect the current release state of the applications from which the benchmarks were derived.  This version introduces latency metrics for the request-based workloads, and basic statistics that characterize each benchmark.  These changes are consistent with the original goals of the DaCapo project, which include the desire for the suite to remain relevant and reflect the current state of deployed Java applications.

# 2. Usage

## 2.1 Downloading

* DaCapo now ships as a zip file which contains a jar file and a folder which contains all data sources and jar files used by the respective workloads.
* Download from https://download.dacapobench.org/chopin/dacapo-23.11-MR1-chopin.zip
* Unzip the zip file

## 2.2 Running

* It is essential that you read and observe the usage guidelines that appear in the [README](https://github.com/dacapobench/dacapobench/blob/main/README.md).
* Run a benchmark: `java -jar dacapo-23.11-MR1-chopin.jar <benchmark>`
* For usage information, run with no arguments.

## 2.3 Compatability

* All benchmarks are compatable with Java 11 through Java 21 (cassandra and h2o require defaults to be overridden via runtime properties).
* All benchmarks aside from biojava, cassandra, eclipse, h2, luindex, lusearch, tomcat, tradebeans and tradesoap are compatable with Java 8.
* The suite has been extensively tested on JDKs from 8 to 21 on Linux.  While we are not aware of any issues on MacOS or Windows, these platforms have not been tested the same way.  Thus we cannot vouch for the stability of the suite on MacOS or Windows (see 4.1 below).

## 2.4 Building from Source

* You must have a working, recent version of ant installed. Change to the benchmarks directory and then run:  `ant -p` for instructions on how to build.

# 3. Changes

## 3.1 Changes introduced by 23.11-MR1

As a maintence release, the set of workloads and their versions were unchanged.   The only changes to workloads were bug fixes which we deemed essential ([**#230**](https://github.com/dacapobench/dacapobench/issues/230), [**#258**](https://github.com/dacapobench/dacapobench/issues/258), [**#264**](https://github.com/dacapobench/dacapobench/issues/264), [**#272**](https://github.com/dacapobench/dacapobench/issues/272), [**#302**](https://github.com/dacapobench/dacapobench/issues/302), [**#304**](https://github.com/dacapobench/dacapobench/issues/304), [**#309**](https://github.com/dacapobench/dacapobench/pull/309)).

### 3.2.1 Features

This release includes a number of significant improvements to the usability of the suite.

* Standalone launcher jars in support of AOT systems such as NativeImage ([**#276**](https://github.com/dacapobench/dacapobench/pull/276)).
  * These can be found in `dacapo-23.11-MR1-chopin/launchers`.
* New metrics for request-based workloads.
  * Separately time the request-based portion of request-based workloads, output elapsed time and throughput for requests ([**#250**](https://github.com/dacapobench/dacapobench/issues/250)).
  * Add server-side request callback hooks ([**#303**](https://github.com/dacapobench/dacapobench/pull/303])).
  * Generalize the latency metrics to include the concept of a smoothing window for metered latency, adding 100ms smoothing to the standard output ([**#283**](https://github.com/dacapobench/dacapobench/issues/283)).
  * Added worker IDs to latency csv's ([**#271**](https://github.com/dacapobench/dacapobench/issues/271)).
* Additions to the nominal statistics including microarchitectural sensitivity and sensitivity to compiler configuration ([**#240**](https://github.com/dacapobench/dacapobench/issues/240)).
* Make all per-benchmark statistics available and visible in `dacapo-23.11-MR1-chopin/stats`.

### 3.1.2 Bug fixes

* [**#313**](https://github.com/dacapobench/dacapobench/pull/313) Fix upstream h2o build issue
* [**#312**](https://github.com/dacapobench/dacapobench/pull/312) Fix spring not working with bytecode analyzer
* [**#309**](https://github.com/dacapobench/dacapobench/pull/309) Integrate h2 [#4124](https://github.com/h2database/h2database/issues/4124) into DaCapo
* [**#306**](https://github.com/dacapobench/dacapobench/issues/306) Don't output latency stats on failed runs
* [**#304**](https://github.com/dacapobench/dacapobench/issues/304) tradebeans and tradesoap should populate data in prepare
* [**#301**](https://github.com/dacapobench/dacapobench/issues/301) git diff-index leading to spurious dirty labelling
* [**#297**](https://github.com/dacapobench/dacapobench/issues/297) Server side pre-request hook in tomcat does not work
* [**#296**](https://github.com/dacapobench/dacapobench/issues/296) h2o build issues
* [**#295**](https://github.com/dacapobench/dacapobench/issues/295) Latency reporter increasing minheaps
* [**#294**](https://github.com/dacapobench/dacapobench/issues/294) Indicate dirty builds alongside git hash
* [**#283**](https://github.com/dacapobench/dacapobench/issues/283) Metered latency orders of magnitude too high?
* [**#282**](https://github.com/dacapobench/dacapobench/issues/282) BCC callback removed?
* [**#281**](https://github.com/dacapobench/dacapobench/issues/281) The benchmark kafka got an 'InvocationTargetException'
* [**#279**](https://github.com/dacapobench/dacapobench/issues/279) Recent changes in LatencyReporter is causing an index out of bounds
* [**#277**](https://github.com/dacapobench/dacapobench/issues/277) Some queries reporting 0 time
* [**#275**](https://github.com/dacapobench/dacapobench/issues/275) Use Ampere Altra for the ARM nominal stats
* [**#274**](https://github.com/dacapobench/dacapobench/issues/274) Benchmark output should go to stdout
* [**#272**](https://github.com/dacapobench/dacapobench/issues/272) YCSB client seems to have debug log enabled
* [**#271**](https://github.com/dacapobench/dacapobench/issues/271) Consider saving worker thread IDs in the latency CSV dump
* [**#270**](https://github.com/dacapobench/dacapobench/issues/270) commons-logging-1.2 not found when building dacapo
* [**#267**](https://github.com/dacapobench/dacapobench/issues/267) Minor issues with metered latency
* [**#266**](https://github.com/dacapobench/dacapobench/issues/266) Metered tail latency not stable in h2 due to slow running threads
* [**#265**](https://github.com/dacapobench/dacapobench/issues/265) --sizes option outputs to stderr
* [**#264**](https://github.com/dacapobench/dacapobench/issues/264) JDK 21, lusearch, and Lucene "regression"
* [**#259**](https://github.com/dacapobench/dacapobench/issues/259) h2o 3.42.0.2 now requires JDK > 8
* [**#258**](https://github.com/dacapobench/dacapobench/issues/258) Race condition in Sunflow benchmark
* [**#253**](https://github.com/dacapobench/dacapobench/issues/253) Cassandra fails to run on aarch64
* [**#252**](https://github.com/dacapobench/dacapobench/issues/252) tradebeans and tradesoap won't start using OpenJDK > 21
* [**#251**](https://github.com/dacapobench/dacapobench/issues/251) Update bytecode statistics
* [**#250**](https://github.com/dacapobench/dacapobench/issues/250) For request-based workloads, report request throughput separately
* [**#240**](https://github.com/dacapobench/dacapobench/issues/240) Richer nominal stats for Chopin

## 3.2 Changes introduced by 23.11

### 3.2.1 Version changes introduced by 23.11

All benchmarks updated to reflect recent versions:

| Benchmark |        9.12 |    23.11 |
|-----------|-------------|----------|
| avrora    |    20091224 | 20131011 |
| batik     |         1.7 |     1.16 |
| eclipse   |       3.5.1 |     4.27 |
| fop       |        0.95 |      2.8 |
| h2        |         1.5 |  2.2.220 |
| jython    |       2.5.2 |    2.7.3 |
| pmd       |       4.2.5 |   6.55.0 |
| tomcat    |       6.0.2 |  10.1.11 |
| daytrader |       2.4.1 |      3.0 |
| xalan     |       2.7.1 |    2.7.2 |
| lucene    |       2.4.1 |    9.7.0 |


### 3.2.2 Benchmark additions due to 23.11

**biojava**: BioJava is an open-source project dedicated to   providing a Java framework for processing biological data.  It provides analytical and statistical routines, parsers for common file formats, reference implementations of popular algorithms, and allows the manipulation of sequences and 3D structures.
      
**cassandra**: Apache Cassandra is a free and open-source, distributed, wide column store, NoSQL database management system designed to handle large amounts of data across many commodity servers.
	   
**graphchi**: GraphChi is a disk-based system for computing  efficiently on graphs with billions of edges, by using  parallel sliding windows method on smaller shards of graphs.

**h2o**: H2O is an in-memory platform for distributed, scalable  machine learning.

**jme**: jMonkeyEngine is a 3D game engine for adventurous Java developers. It’s open-source, cross-platform, and cutting-edge.
	          
**kafka**: Apache Kafka aims to provide a unified, high-throughput, low-latency platform for handling real-time data feeds.

**spring**: A Spring Boot application that models a 'pet clinic'  which allows users to book pets into a fictitious veterinary  clinic via a web server.

**zxing**: Zxing is a multi-format 1D/2D barcode image processing library

### 3.2.3. Benchmark deletions due to 23.11

**lusearch-fix**:  This is no longer needed as we've moved to a much more recent version of Lucene.
       
### 3.2.4. Other Notable Changes due to 23.11

Latency metrics are now reported for each of the request-based workloads  (cassandra, h2, kafka, lusearch, spring, tomcat, tradebeans and tradesoap) and jme, which is a rendering workload (game engine).  The benchmark harness internally records the period of each request (or frame render), and at the end of each benchmark iteration outputs percentile statistics for request latencies. We report two metrics. Simple latency just reports the raw percentile latencies that were observed. Metered latency reports latencies as seen via an emulated request queue and a continuous stream of requests. Simple latency is unrealistic in that the rate at which requests are accepted is a function of the rate at which they are processed. In real systems requests will be enqueued and some of the user-observed latency is due to requests waiting in the request queue.

Each of the workloads has been measured against a variety of metrics, and the statistics are recorded in the DaCapo git repository at 
benchmarks/bm/*/stats-*.yml. When benchmarks are invoked some summary metrics are displayed giving a score (out of 10), an absolute value for each metric, its rank among the other benchmarks, and a short description.  This allows some level of benchmark characterization and benchmark comparison to be done.

The benchmark harness now supports a watchdog timer which can be set with the `-w` command line option.  This will terminate the JVM after the specified number of seconds if the process has not already completed.  This can be useful for debugging, for terminating runaway tasks in set amount of time.

## 3.3 Changes introduced by 9.12-MR1

**lusearch-fix** is introduced as a new benchmark.   The lusearch-fix  and lusearch benchmarks differ by a single line of code.   This is a bug fix to lucene, which dramatically changes the performance of lusearch, reducing the amount of allocation greatly. https://issues.apache.org/jira/browse/LUCENE-1800 https://dl.acm.org/citation.cfm?id=2048092 We encourage you to use lusearch-fix in place of lusearch.   We retain the unpatched lusearch in this release for historical consistency.

 URLs used by the build system have been systematically updated so that the source distribution works correctly.
	      
Other issues in the source distribution have been fixed to ensure that the suite builds with Java 8 VMs.
	      
## 3.4 Changes introduced by 9.12

### 3.4.1 Version changes introduced by 9.12

All benchmark versions were updated.

### 3.4.2 Benchmark additions due to 9.12

**avrora**: AVRORA is a set of simulation and analysis tools in a framework for AVR micro-controllers. The benchmark exhibits a great deal of fine-grained concurrency. The  benchmark is courtesy of Ben Titzer (Sun Microsystems)  and was developed at UCLA.

**batik**: Batik is an SVG toolkit produced by the Apache foundation. The benchmark renders a number of svg files.

**h2**: h2 is an in-memory database benchmark, using the h2 database produced by h2database.com, and executing an implementation of the TPC-C workload produced by the Apache foundation for its derby project. h2 replaces derby, which in turn replaced hsqldb.

**sunflow**: Sunflow is a raytracing rendering system for photo-realistic images.

**tomcat**: Tomcat uses the Apache Tomcat servelet container to run some sample web applications.

**tradebeans**: Tradebeans runs the Apache daytrader workload "directly" (via EJB) within a Geronimo application server.  Daytrader is derived from the IBM Trade6 benchmark.

**tradesoap**: Tradesoap is identical to the tradebeans workload, except that client/server communications is via soap protocols (and the workloads are reduced in size to compensate the substantially higher overhead).

Tradebeans and tradesoap were intentionally added as a pair to allow researchers to evaluate and analyze the overheads and behavior of communicating through a protocol such as SOAP.  Tradesoap's "large" configuration uses exactly the same workload as tradebeans' "default" configuration, and tradesoap's "huge" uses exactly the same workload as tradebeans' "large", allowing researchers to directly compare the two systems.

### 3.4.3 Benchmark deletions due to 9.12

**antlr**: Antlr is single threaded and highly repetitive. The most recent version of jython uses antlr; so antlr remains represented within the DaCapo suite.

**bloat**: Bloat is not as widely used as our other workloads and the code exhibited some pathologies that were arguably not representative or desirable in a suite that was to be representative of modern Java applications.

 **chart**: Chart was repetitive and used a framework that appears  not to be as widely used as most of the other DaCapo  benchmarks.  The Batik workload has some similarities with chart (both are render vector graphics), but is part of a larger heavily used framework from Apache.

**derby**: Derby has been replaced by h2, which runs a much richer workload and uses a more widely used and higher performing database engine (derby was not in any previous release, but had been slated for inclusion in  this release).

**hsqldb**: Hsqldb has been replaced by h2, which runs a much richer workload and uses a more widely used and higher  performing database engine.

### 3.4.4 Other Notable Changes due to 9.12

The packaging of the DaCapo suite was been completely re-worked and the source code is entirely re-organized for 9.12.

We've changed the naming scheme for the releases.  Rather than "dacapo-YYYY-MM", we've moved to "dacapo-Y.M-TAG", where TAG is a nickname for the release.  Given the theme for this project, we're using musical names, and since this release is our second, we've given this one the nick-name "bach".  The release can therefore be referred to by its nickname, which rolls off the tounge a little more easily than our old names.  Of course we've borrowed this scheme from other projects (such as Ubuntu) which follow a similar pattern.

The command-line arguments have be rationalized and now follow posix conventions.

Threading has been rationalized.   Benchmarks are now characterized in terms of their external and internal concurrency.  (For example
a benchmark such as eclipse is single-threaded externally, but internally uses a thread pool).   All benchmarks which are externally
multi-threaded now by default run a number of threads scaled to match the available processors, and the number of externally defined
threads may also be configured via the "-t" and "-k" command line options which specify, respectively the absolute number of external
threads and a multiplier against the number of available processors. Some benchmarks are both internally and externally multithreaded,
such as tradebeans and tradesoap, where the number of client threads may be specified externally, but the number of server threads is
determined within the server, and cannot be directly controlled by the user.

We have introduced a "huge" size for a number of benchmarks, which scales the workload to run for much longer and consume significant memory.  We have also retired "large" sizes for some benchmarks where "large" was not distinctly different from "default".  Thus there are now four sizes: "small", "default", "large", and "huge", and "large" and "huge" are only available for some benchmarks.  If you attempt to run a benchmark at an unsupported size you will get an error message.

# 4. Known Issues

Please consult the bug tracker for a complete and up-to-date list of known issues (https://github.com/dacapobench/dacapobench/issues).

DaCapo is an open source community project. We welcome all assistance in addressing bugs and shortcomings in the suite.

A few notable unresolved high priority issues are listed here:

## 4.1 No extensive testing on MacOS or Windows

DaCapo should perform equally well on Linux, MacOS and Windows.  However, due to our limited resources, our testing has been limited to Linux, covering JDKs 8 - 21 from various vendors.

## 4.2 Socket use by tradebeans, tradesoap and tomcat

Each of these benchmarks use sockets to communicate between their clients and server.  We have observed that connections are used very liberally (we have seen more than 64,000 connections in use when running tradebeans in its "huge" configuration, according to netstat). We believe that this phenomena can lead to spurious failures, particularly on tradesoap, where the benchmark fails with an error message that indicates a garbled bean (stock name seen when userid expected).  At the time of writing, we believe these issues are platform-sensitive and are due to the underlying systems rather than our particular use of them.  As with all issues, we welcome feedback and fixes from the community.

## 4.3 Validation

Validation continues to use summarization via a checksum, so we are unable to provide a diff between expected and actual output in the
case of failure.   We hope to update this, and welcome community contributions.

## 4.4 Support for whole-program static analysis

Despite significant help from the community, we have had to drop support for whole-program static analysis that was available in the
last major release.  The main reason for this is that the more systematic and extensive use of reflection and the enormous internal
complexity of workloads such as tradebeans and tradesoap has made it very difficult to produce a straightforward mechanism that would facilitate such analyses.  While we regret this omission, such an addition should have no effect on the workloads themselves.
Therefore, if the community is able to contribute enhancements or extensions to the suite that facilitate such static analysis, we
should be able to include such a contribution in a maintenance release, rather than having to wait for the next major release of the DaCapo benchmark suite.

# 5. Contributions and Acknowledgements

The support of the Australian National University, Oracle, Google, and Huawei was crucial to the successful completeion of the 23.11 and 9.12-MR1 releases.

The 23.11 release was led by Steve Blackburn of ANU and Google, and developed primarily by:

*  Steve Blackburn, Google and Australian National University
*  Rui Chen, Australian National University
*  John Zhang, Australian National University

In preparing the 23.11 release we received guidance and assistance from a number of people including:

* Xi Yang, Australian National University, Confluent and Twitter
* Zixian Cai, Australian National University
* Qiqi Chang, Australian National University
* Aditya Chilukuri, Australian National University
* Sichao Li, Australian National University
* Yiyi Shao, Australian National University
* Enming Zhang, Australian National University
* Leandro Watanabe, University of Utah
* JC Beylor, Google
* Kathryn McKinley, Google
* Wessam Hassanein, Google
* Ryan Rose, Google
* Cliff Click

Many other people provided valuable feedback, bug fixes and advice.