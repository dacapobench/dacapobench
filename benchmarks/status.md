|bm | updated | jdk 8 | jdk 11 | clean | scratch | validation | small | default | large | huge | long | latency | off heap?
|-|-|-|-|-|-|-|-|-|-|-|-|-|-|
|avrora|n/a|✓|✓|✓|✓|✓|✓|✓|✓|||
|batik|✓|✓|✓|✓|✓|✓|✓|✓|✓|✓||
|biojava|✓|✓|✓|✓|✓|✓|✓|✓|✓|✓||
|cassandra|✓|✓|✓|✓|✓|✓|✓|✓|✓|✓|✓|✓|
|eclipse|✓|✓|✓|✓|✓|✓|✓|✓|✓|||
|fop|✓|✓|✓|✓|✓|✓|✓|✓||||
|graphchi|n/a|✓|✓|✓|✓|✓|✓|✓|✓|✓||||
|h2|✓|✓|✓|✓|✓|✓|✓|✓|✓|✓||✓|
|h2o|✓|✓|✓|✓|✓|✓|✓|✓|✓|✓|||
|jme|✓|✓|✓|✓|✓|✓|✓|✓|✓||✓|✓|
|jython|n/a|✓|✓|✓|✓|✓|✓|✓|✓||✓||
|kafka|✓|✓|||✓|✓|✓|✓|✓||?|✓|
|luindex|✓|✓|✓|✓|✓|✓|✓|✓||✓|||
|lusearch|✓|✓|✓||✓|✓|✓|✓|✓|✓|✓|✓|
|pmd|✓|✓|✓|✓|✓|✓|✓|✓|✓|||
|sunflow|n/a|✓|✓|✓|✓|✓|✓|✓|✓|||
|tomcat|✓|✓|✓|✓|✓|✓|✓|✓|✓||✓|✓|
|tradebeans|✓|✓|✓|✓|✓|✓|✓|✓|✓|✓|✓|✓|
|tradesoap|✓|✓|✓|✓|✓|✓|✓|✓|✓|✓|✓|✓|
|xalan|n/a|✓|✓|✓|✓|✓|✓|✓|✓|||||
|zxing|✓|✓|✓|✓|✓|✓|✓|✓||||||


### TODO
* Priorities
  * Soon
    * Audit huge workloads
    * Finalize packaging (zip without huge, huge auto-downloaded on demand) 
    * Add dilation command line option
    * Kafka not working with JDK11+ https://stackoverflow.com/questions/46920461/java-lang-noclassdeffounderror-javax-activation-datasource-on-wsimport-intellij https://stackoverflow.com/questions/52921879/migration-to-jdk-11-has-error-occure-java-lang-noclassdeffounderror-javax-acti
    * Cassandra does not work with Java 15 due to accessing a hidden class (eg  https://github.com/doanduyhai/Achilles/issues/372 and https://issues.apache.org/jira/browse/CASSANDRA-16304)
    * jme is a beta release, update once stable (3.4.0-beta2)
    * Fix instructions at top of build.xml -- completely wrong
    * Update size names
    * Investigate biojava, cassandra, lusearch perf issues
    * check lusearch xlarge tx overflow error (latency buffer sizing)
* biojava fails validation on later releases due to a WARNING message triggered by the following: https://issues.apache.org/jira/browse/LOG4J2-2537
* checkout why cpu utilization for lusearch large is so variable (dips to 200% then sometimes 2000%)
* Move standard workloads out of wasabi src, into github (audit them for size first)

* Packaging
  * create a single dacapo zip that contains the jar, the dat and jar folders
* Biojava: why do we not see any parallelism? [maybe this](https://bugs.openjdk.java.net/browse/JDK-8247980)
* Cassandra: further calibration required.   Not clear that workload configs are affecting heap size.

* H2
  * should be built from source, is not
* JME
  * Check reason for low CPU useage (GPU?)
* Kafka 
  * why is CPU utilization low?
* Update parser to use 'verbose' rather than 'long' for the description field, allowing 'long' to be used as a size.
* Latency:
  * Bound buffer size.   Fill it up then sample to it afterwards.  Allow size to be specified on command line.
  * Create buffers in harness in a uniform way
  * Always provide a latency buffer, and optionally start and thread id buffers
  * only go to file if requested, otherwise do it all in memory with pre-allocated buffer
* Lusearch and index
  * Recalibrate using wiki data (smaller samples)

Validation FAILED for jme xlong

real	5m38.646s
user	0m4.070s
sys	0m0.307s


h2 default: 6794 18s user
h2 large: 26s user (fail)
h2 huge: 32s
h2 xlarge 2m real
h2 'dots' too verbose for large settings

No good
		"--scale","128",
		"--cleanup-in-iteration"
  thread-limit 256000

1min
  		"--total-transactions","128000",
		"--scale","64",
		"--cleanup-in-iteration"

Stopped after 18 mins (had not generated db fully)
    size xlong args 
		"--total-transactions","4096000",
		"--scale","64",
		"--cleanup-in-iteration"

Stopped after 
    size xlong args 
		"--total-transactions","4096000",
		"--scale","48",
		"--cleanup-in-iteration"

xlong2
real	1m9.247s
user	7m59.654s
sys	0m3.362s

xlong3
real	1m21.654s
user	9m8.678s
sys	0m3.979s

xlong6
real	1m27.738s
user	10m9.393s
sys	0m3.844s

xlong7
real	1m38.758s
user	11m22.263s
sys	0m4.204s

real	1m21.079s
user	8m50.431s
sys	0m3.713s