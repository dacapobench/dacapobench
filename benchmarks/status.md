|bm | updated | jdk 8 | jdk 11 | clean | scratch | validation | small | default | large | huge | long | latency |
|-|-|-|-|-|-|-|-|-|-|-|-|-|
|avrora|✓|✓|✓|✓|✓|✓|✓|✓|✓|||
|batik|✓|✓|✓|✓|✓|✓|✓|✓|✓|✓||
|biojava|✓|✓|✓|✓|✓|✓|✓|✓|✓|✓||
|cassandra|✓|✓|✓|✓|✓|✓|✓|✓|✓|✓|✓|✓|
|eclipse|✓|✓|✓|✓|✓|✓|✓|✓|✓|||
|fop|✓|✓|✓|✓|✓|✓|✓|✓||||
|graphchi|✓|✓|✓|✓|✓|✓|✓|✓|✓|✓|||?|
|h2|✓|✓|✓|✓|✓|✓|✓|✓|✓|✓||✓|
|h2o|✓|✓|✓|✓|✓|✓|✓|✓|✓|✓||?|
|jme|✓|✓|✓|✓|✓|✓|✓|✓|✓||✓|✓|
|jython|✓|✓|✓|✓|✓|✓|✓|✓|✓||✓||
|kafka|✓|✓|✓||✓|✓||✓|||?|?|
|sunflow|✓|✓|✓|✓|✓|✓|✓|✓|✓|||


### TODO
* Data is read only
  * check that benchmarks never write to dat director
* Packaging
  * create a single dacapo zip that contains the jar, the dat and jar folders
  * list of jars is duplicated, one automatic via build, plus old one that is in cnf, DaCapoClassLoader depends on the latter.
* Biojava: why do we not see any parallelism? [maybe this](https://bugs.openjdk.java.net/browse/JDK-8247980)
* Cassandra: further calibration required.   Not clear that workload configs are affecting heap size.
* Eclipse
  * Check that build is not happening in DATA
* H2 
  * move latency file writes out of timing loop (like cassandra)
  * should be built from source, is not
* JME
  * Check reason for low CPU useage (GPU?)
* Update parser to use 'verbose' rather than 'long' for the description field, allowing 'long' to be used as a size.
\\


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