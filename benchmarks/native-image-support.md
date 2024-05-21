## Summary

We followed the approach in renaissance
(https://github.com/renaissance-benchmarks/renaissance/pull/341) to create one launcher JAR for each
benchmark. The launcher JARs are located in the directory `launchers`:

```
dat/       jar/       launchers/
```

For example, the launcher JAR for `sunflow` only contains the file `META-INF/MANIFEST.MF`:

```
Manifest-Version: 1.0
Main-Class: Harness
Class-Path: ../jar/lib/janino/janino-2.5.15.jar ../jar/sunflow/sunflow-0.07.2.jar ../../dacapo-evaluation-git-82543f43.jar
```

To run the benchmark with `java`, it suffices to issue the following command:

```
java -jar path/to/launchers/sunflow.jar
```

## Usage with Native Image

To compile the launcher JAR with Native Image, we need to first run the agent to collect the meta
data about reflections and resources:

```
path/to/graalvm/bin/java -agentlib:native-image-agent=config-output-dir=./dacapo -jar  path/to/launchers/sunflow.jar sunflow
```

With the refleciton configuration collected under the directory `./dacapo`, we can now compile it with Native Image:

```
path/to/graalvm/bin/native-image -H:ConfigurationFileDirectories=./dacapo -jar path/to/launchers/sunflow.jar
```

Now we can run the compiled executable:

```
./sunflow
```

##  Implementation

Link:  https://github.com/dacapobench/dacapobench/compare/main...liufengyun:fliu/standalone-launcher-jar

We tried to minimize the changes to Dacapo --- the build workflow is exactly the same as before.

The main change is that we added a python script to create the launcher JAR when building the
distribution of a benchmark.

The python script only depends on a built-in library `zipfile` of Python. We have checked that it is
present for all active releases of Python from `3.8` to `3.12`.
