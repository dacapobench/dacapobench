|bm | updated | jdk 8 | jdk 11 | clean | scratch | validation | small | default | large | huge | latency |
|-|-|-|-|-|-|-|-|-|-|-|-|
|avrora|✓|✓|✓|✓|✓|✓|✓|✓|✓|||
|batik|✓|✓|✓|✓|✓|✓|✓|✓|✓|✓||
|biojava|✓|✓|✓|✓|✓|✓|✓|✓|✓|✓||
|cassandra|✓|✓|✓|✓|✓|✓|✓|✓|✓|?|?|
|eclipse|✓|✓|✓|✓|✓|✓|✓|✓|✓|||
|fop|✓|✓|✓|✓|✓|✓|✓|✓||||
|graphchi|✓|✓|✓|✓|✓|✓|✓|✓|✓|✓||
|h2|✓|✓|✓|✓|✓|✓|✓|✓|✓|✓|?|
|h2o|✓|✓|✓|✓|✓|✓|✓|✓|✓|✓|?|
|jme|✓|✓|✓|✓|✓|✓|✓|✓|✓||✓|
|jython|✓|✓|✓||✓|✓|✓|✓|✓|||
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
  * should be built from source, is not
  * shoudl output message saying database is being constructed
* JME
  * Check reason for low CPU useage (GPU?)
* Jython
  * Turn off excepton on high warp factor (for small): "warp factor too high: test times are < 10ms"
  * Clean up output


