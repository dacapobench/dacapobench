# dacapo-23-7-chopin  RC1 RELEASE NOTES  2023-07

This is the first release candidate of the third major release of the DaCapo benchmark suite.

*We strongly welcome feedback, bug fixes and suggestions.   Please do this via [github](https://github.com/dacapobench/dacapobench)*

These notes are structured as follows:

1. Overview
2. Usage
3. Changes
4. Known problems and limitations
5. Contributions and Acknowledgements

# 1. Overview

The 23.07 release is the third major update of the suite.  It is strictly incompatible with previous releases: new benchmarks have been added, old benchmarks have been removed, all other benchmarks have been substantially updated and the inputs have changed for every program. It is for this reason that in any published use of the suite, the version of the suite must be explicitly stated.

The release sees the move from Geronimo to JBoss Wildfly for the trade benchmarks, the addition of eight completely new benchmarks: biojava, cassandra, graphchi, h2o, jme, kafka, spring, and zxing, and the upgrade of all other benchmarks to reflect the current release state of the applications from which the benchmarks were derived.  This version introduces latency metrics for the request-based workloads, and basic statistics that characterize each benchmark.  These changes are consistent with the original goals of the DaCapo project, which include the desire for the suite to remain relevant and reflect the current state of deployed Java applications.


# 2. Usage

## 2.1 Downloading

* Download from git via ttps://github.com/dacapobench/dacapobench

## 2.2 Running

* It is essential that you read and observe the usage guidelines that appear in README.md
* Run a benchmark: `java -jar <dacapo-jar-name>.jar <benchmark>`
* For usage information, run with no arguments.
  
## 2.3 Building

* You must have a working, recent version of ant installed. Change to the benchmarks directory and then run:  `ant -p` for instructions on how to build.

# 3. Changes

## 3.1 Changes introduced by 23.07

### 3.1.1 Version changes introduced by 23.07

All benchmarks updated to reflect recent versions:

| Benchmark | 9.12 | 23.07 |
|---|---|---|
|avrora|    20091224 | 20131011 |
| batik |    1.7     | 1.16 |
| eclipse |   3.5.1   | 4.25 |
| fop |      0.95     | 2.8 |
| h2|        1.5      | 2.1.214 |
| jython|   2.5.2   | 2.7.3 |
| pmd|     4.2.5  | 6.52.0 |
| tomcat| 6.0.2   | 10.0.27 |
| daytrader| 2.4.1   | 3.0-SNAPSHOT |
| xalan|     2.7.1   |2.7.2 |
| lucene|   2.4.1    | 9.4 |

	
### 3.1.2 Benchmark additions due to 23.07

**biojava**: BioJava is an open-source project dedicated to   providing a Java framework for processing biological data.  It provides analytical and statistical routines, parsers for common file formats, reference implementations of popular algorithms, and allows the manipulation of sequences and 3D structures.
      
**cassandra**: Apache Cassandra is a free and open-source, distributed, wide column store, NoSQL database management system designed to handle large amounts of data across many commodity servers.
	   
**graphchi**: GraphChi is a disk-based system for computing  efficiently on graphs with billions of edges, by using  parallel sliding windows method on smaller shards of graphs.

**h2o**: H2O is an in-memory platform for distributed, scalable  machine learning.

**jme**: jMonkeyEngine is a 3D game engine for adventurous Java developers. It’s open-source, cross-platform, and cutting-edge.
	          
**kafka**: Apache Kafka aims to provide a unified, high-throughput, low-latency platform for handling real-time data feeds.

**spring**: A Spring Boot application that models a 'pet clinic'  which allows users to book pets into a fictitious veterinary  clinic via a web server.

**zxing**: Zxing is a multi-format 1D/2D barcode image processing library

### 3.1.3. Benchmark deletions due to 29.07

**lusearch-fix**:  This is no longer needed as we've moved to a much more recent version of Lucene.
       
### 3.1.4. Other Notable Changes due to 29.07

Latency metrics are now reported for each of the request based workloads  (cassandra, h2, kafka, lusearch, spring, tomcat, tradebeans and tradesoap) and jme, which is a rendering workload (game engine).  The benchmark harness internally records the period of each request (or frame render), and at the end of each benchmark iteration outputs percentile statistics for request latencies. We report two metrics. Simple latency just reports the raw percentile latencies that were observed. Metered latency reports latencies as seen via an emulated request queue and a continuous stream of requests. Simple latency is unrealistic in that the rate at which requests are accepted is a function of the rate at which they are processed. In real systems requests will be enqueued and some of the user-observed latency is due to requests waiting in the request queue.

Each of the workloads has been measured against a variety of metrics, and the statistics are recorded in the project's git repository at 
benchmarks/bm/*/stats-*.yml. When benchmarks are invoked some summary metrics are displayed giving a score (out of 10) and an absolute value for each metric for each benchmark.  This allows some level of benchmark characterization and benchmark comparison to be done.

## 3.2 Changes introduced by 9.12-MR1

**lusearch-fix** is introduced as a new benchmark.   The lusearch-fix  and lusearch benchmarks differ by a single line of code.   This is a bug fix to lucene, which dramatically changes the performance of lusearch, reducing the amount of allocation greatly. https://issues.apache.org/jira/browse/LUCENE-1800 https://dl.acm.org/citation.cfm?id=2048092 We encourage you to use lusearch-fix in place of lusearch.   We retain the unpatched lusearch in this release for historical consistency.

 URLs used by the build system have been systematically updated so that the source distribution works correctly.
	      
Other issues in the source distribution have been fixed to ensure that the suite builds with Java 8 VMs.
	      
## 3.3 Changes introduced by 9.12

### 3.3.1 Version changes introduced by 9.12

All benchmark versions were updated.

### 3.3.2 Benchmark additions due to 9.12

**avrora**: AVRORA is a set of simulation and analysis tools in a framework for AVR micro-controllers. The benchmark exhibits a great deal of fine-grained concurrency. The  benchmark is courtesy of Ben Titzer (Sun Microsystems)  and was developed at UCLA.

**batik**: Batik is an SVG toolkit produced by the Apache foundation. The benchmark renders a number of svg files.

**h2**: h2 is an in-memory database benchmark, using the h2 database produced by h2database.com, and executing an implementation of the TPC-C workload produced by the Apache foundation for its derby project. h2 replaces derby, which in turn replaced hsqldb.

**sunflow**: Sunflow is a raytracing rendering system for photo-realistic images.

**tomcat**: Tomcat uses the Apache Tomcat servelet container to run some sample web applications.

**tradebeans**: Tradebeans runs the Apache daytrader workload "directly" (via EJB) within a Geronimo application server.  Daytrader is derived from the IBM Trade6 benchmark.

**tradesoap**: Tradesoap is identical to the tradebeans workload, except that client/server communications is via soap protocols (and the workloads are reduced in size to compensate the substantially higher overhead).

Tradebeans and tradesoap were intentionally added as a pair to allow researchers to evaluate and analyze the overheads and behavior of communicating through a protocol such as SOAP.  Tradesoap's "large" configuration uses exactly the same workload as tradebeans' "default" configuration, and tradesoap's "huge" uses exactly the same workload as tradebeans' "large", allowing researchers to directly compare the two systems.

### 3.3.3 Benchmark deletions due to 9.12

**antlr**: Antlr is single threaded and highly repetitive. The most recent version of jython uses antlr; so antlr remains represented within the DaCapo suite.

**bloat**: Bloat is not as widely used as our other workloads and the code exhibited some pathologies that were arguably not representative or desirable in a suite that was to be representative of modern Java applications.

 **chart**: Chart was repetitive and used a framework that appears  not to be as widely used as most of the other DaCapo  benchmarks.  The Batik workload has some similarities with chart (both are render vector graphics), but is part of a larger heavily used framework from Apache.

**derby**: Derby has been replaced by h2, which runs a much richer workload and uses a more widely used and higher performing database engine (derby was not in any previous release, but had been slated for inclusion in  this release).

**hsqldb**: Hsqldb has been replaced by h2, which runs a much richer workload and uses a more widely used and higher  performing database engine.

### 3.3.4 Other Notable Changes due to 9.12

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

## 4.1 Socket use by tradebeans, tradesoap and tomcat

Each of these benchmarks use sockets to communicate between their clients and server.  We have observed that connections are used very liberally (we have seen more than 64,000 connections in use when running tradebeans in its "huge" configuration, according to netstat). We believe that this phenomena can lead to spurious failures, particularly on tradesoap, where the benchmark fails with an error message that indicates a garbled bean (stock name seen when userid expected).  At the time of writing, we believe these issues are platform-sensitive and are due to the underlying systems rather than our particular use of them.  As with all issues, we welcome feedback and fixes from the community.

## 4.2 Validation

Validation continues to use summarization via a checksum, so we are unable to provide a diff between expected and actual output in the
case of failure.   We hope to update this, and welcome community contributions.


## 4.3 Support for whole-program static analysis

Despite significant help from the community, we have had to drop support for whole-program static analysis that was available in the
last major release.  The main reason for this is that the more systematic and extensive use of reflection and the enormous internal
complexity of workloads such as tradebeans and tradesoap has made it very difficult to produce a straightforward mechanism that would facilitate such analyses.  While we regret this omission, such an addition should have no effect on the workloads themselves.
Therefore, if the community is able to contribute enhancements or extensions to the suite that facilitate such static analysis, we
should be able to include such a contribution in a maintenance release, rather than having to wait for the next major release of the DaCapo benchmark suite.

# 5. Contributions and Acknowledgements

The support of the Australian National University, Oracle, Google, and Huawei was crucial to the successful completeion of the 23.07 and 9.12-MR1 releases.

The 23.07 release was led by Steve Blackburn of ANU and Google, and developed primarily by:

*  Steve Blackburn, Google and Australian National University
*  Rui Chen, Australian National University
*  John Zhang, Australian National University

In preparing the 23.07 release we received guidance and assistance from a number of people including:

* Xi Yang, Australian National University, Confluent and Twitter
* Zixian Cai, Australian National University
* Qiqi Chang, Australian National University
* Aditya Chilukuri, Australian National University
* Sichao Li, Australian National University
* Yiyi Shao, Australian National University
* Enming Zhang, Australian National University
* Leaandro Watanabe, University of Utah
* JC Beylor, Google
* Kathryn McKinley, Google
* Wessam Hassanein, Google
* Ryan Rose, Google
* Cliff Click

Many other people provided valuable feedback, bug fixes and advice.