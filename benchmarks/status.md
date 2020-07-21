|bm | updated | jdk 8 | jdk 11 | clean | scratch | validation | small | default | large | huge | long | latency |
|-|-|-|-|-|-|-|-|-|-|-|-|-|
|avrora|n/a|✓|✓|✓|✓|✓|✓|✓|✓|||
|batik|✓|✓|✓|✓|✓|✓|✓|✓|✓|✓||
|biojava|✓|✓|✓|✓|✓|✓|✓|✓|✓|✓||
|cassandra|✓|✓|✓|✓|✓|✓|✓|✓|✓|✓|✓|✓|
|eclipse|✓|✓|✓|✓|✓|✓|✓|✓|✓|||
|fop|✓|✓|✓|✓|✓|✓|✓|✓||||
|graphchi|✓|✓|✓|✓|✓|✓|✓|✓|✓|✓||||
|h2|✓|✓|✓|✓|✓|✓|✓|✓|✓|✓||✓|
|h2o|✓|✓|✓|✓|✓|✓|✓|✓|✓|✓||?|
|jme|✓|✓|✓|✓|✓|✓|✓|✓|✓||✓|✓|
|jython|✓|✓|✓|✓|✓|✓|✓|✓|✓||✓||
|kafka|✓|✓|✓||✓|✓|✓|✓|✓||?|✓|
|luindex|✓|✓|✓|✓|✓|✓|✓|✓||✓|||
|lusearch|✓|✓|✓||✓|✓|✓|✓||✓|✓|?|
|pmd|✓|✓|✓|✓|✓|✓|✓|✓|✓|||
|sunflow|✓|✓|✓|✓|✓|✓|✓|✓|✓|||
|tomcat|✓|✓|✓|✓|✓|✓|✓|✓|✓||✓|?|
|tradebeans|✓|✓|✓|✓|✓|✓|✓|✓|✓|✓|✓|?|
|tradebeans|✓|✓|✓|✓|✓|✓|✓|✓|✓|✓|✓|?|
|xalan|n/a|✓|✓|✓|✓|✓|✓|✓|✓|||||
|zxing|✓|✓|✓|✓|✓|✓|✓|✓||||||


### TODO
* Priorities
  * Immediate
    * Fix / add latency reporting
    * Fix spurious dir creation ({$bm-files})
  * Soon
    * Investiage trade failures
    * Update size names
    * Investigate biojava, cassandra, perf issues
* Check why lusearch failed once the query set was made larger, or at least why the benchmark time got much faster once the queries had 512 entries rather than 256.
* Move standard workloads out of wasabi src, into github (audit them for size first)
* Data is read only
  * check that benchmarks never write to dat director
* Packaging
  * create a single dacapo zip that contains the jar, the dat and jar folders
* Biojava: why do we not see any parallelism? [maybe this](https://bugs.openjdk.java.net/browse/JDK-8247980)
* Cassandra: further calibration required.   Not clear that workload configs are affecting heap size.
* Eclipse
  * Check that build is not happening in DATA
* H2 
  * move latency file writes out of timing loop (like cassandra)
  * should be built from source, is not
* JME
  * Check reason for low CPU useage (GPU?)
* Kafka 
  * why is CPU utilization low?
  * Still one uguly warning message
* Update parser to use 'verbose' rather than 'long' for the description field, allowing 'long' to be used as a size.
* Latency:
  * Bound buffer size.   Fill it up then sample to it afterwards.  Allow size to be specified on command line.
  * Create buffers in harness in a uniform way
  * Always provide a latency buffer, and optionally start and thread id buffers
  * only go to file if requested, otherwise do it all in memory with pre-allocated buffer
* Lusearch and index
  * Recalibrate using wiki data (smaller samples)
* Trade
  * two apparent bugs
    * occasionally see 
```org.h2.jdbc.JdbcSQLException: Unique index or primary key violation: "PRIMARY_KEY_F ON PUBLIC.ACCOUNTPROFILEEJB(USERID) VALUES ('000000', 1)"; SQL statement:
insert into accountprofileejb ( userid, passwd, fullname, address, email, creditcard ) VALUES (  ?  ,  ?  ,  ?  ,  ?  ,  ?  ,  ?  ) [23505-197]
```
    * typically times out on huge setting

  * Stop daytrader/wildfly-17.0.0.Final/standalone/configuration/logging.properties being changed, which probably means turning off logging as per comment at top of that file.
  * Also log indicates failure to find some jars on account of "+" in path name... the fact that this still works suggests that tehre are redundant jars.
  [org.jboss.weld.deployer] (MSC service thread 1-5) WFLYWELD0016: Could not read entries: java.nio.file.NoSuchFileException: /home/steveb/devel/dacapo/dacapobench/benchmarks/dacapo-evaluation-git 89281bd/dat/daytrader/wildfly-17.0.0.Final/modules/system/layers/base/io/smallrye/health/main/smallrye-health-1.0.2.jar
  ```
          at org.jboss.as.weld@17.0.0.Final//org.jboss.as.weld.deployment.processors.UrlScanner.handleArchiveByFile(UrlScanner.java:136)
        at org.jboss.as.weld@17.0.0.Final//org.jboss.as.weld.deployment.processors.UrlScanner.handle(UrlScanner.java:125)
        at org.jboss.as.weld@17.0.0.Final//org.jboss.as.weld.deployment.processors.UrlScanner.scan(UrlScanner.java:89)
        at org.jboss.as.weld@17.0.0.Final//org.jboss.as.weld.deployment.processors.ExternalBeanArchiveProcessor.discover(ExternalBeanArchiveProcessor.java:287)
        at org.jboss.as.weld@17.0.0.Final//org.jboss.as.weld.deployment.processors.ExternalBeanArchiveProcessor.deploy(ExternalBeanArchiveProcessor.java:198)
        at org.jboss.as.server@9.0.1.Final//org.jboss.as.server.deployment.DeploymentUnitPhaseService.start(DeploymentUnitPhaseService.java:176)
        at org.jboss.msc@1.4.7.Final//org.jboss.msc.service.ServiceControllerImpl$StartTask.startService(ServiceControllerImpl.java:1737)
  ```

* WARN:
  * cassandra WARNING: Illegal reflective access by org.apache.cassandra.utils.FBUtilities (file:/home/steveb/devel/dacapo/dacapobench/benchmarks/dacapo-evaluation-git+7cd326b/jar/cassandra/cassandra-3.11.6.jar) to field java.io.FileDescriptor.fd
  * h2o WARNING: Illegal reflective access by ml.dmlc.xgboost4j.java.NativeLibLoader (file:/home/steveb/devel/dacapo/dacapobench/benchmarks/dacapo-evaluation-git+7cd326b/jar/h2o/h2o.jar) to field java.lang.ClassLoader.usr_paths
  * jme WARNING: Illegal reflective access by com.jme3.util.ReflectionAllocator (file:/home/steveb/devel/dacapo/dacapobench/benchmarks/dacapo-evaluation-git+7cd326b/jar/jme/jme3-core.jar) to method sun.nio.ch.DirectBuffer.cleaner()
  * tomcat WARNING: Illegal reflective access by org.apache.catalina.loader.WebappClassLoaderBase (file:/home/steveb/devel/dacapo/dacapobench/benchmarks/dacapo-evaluation-git+7cd326b/jar/tomcat/catalina.jar) to field java.io.ObjectStreamClass$Caches.localDescs

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