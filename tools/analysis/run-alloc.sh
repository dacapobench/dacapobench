#!/usr/bin/bash

VERSION=dacapo-evaluation-git-3745e0c2
BASEDIR=$(cd ../../benchmarks; pwd)
JAVA=/usr/lib/jvm/temurin-21-jdk-amd64/bin/java
JAR=$BASEDIR/$VERSION.jar

for bm in `$JAVA -jar $JAR -l 2>/dev/null`; do
    YML=$BASEDIR/bms/$bm/stats-alloc.yml
    $JAVA -Ddacapo.alloc.yml=$YML -Dsys.ai.h2o.debug.allowJavaVersions=21 -Djava.security.manager=allow -javaagent:$BASEDIR/$VERSION/jar/java-allocation-instrumenter-3.3.4.jar -jar $JAR -callback org.dacapo.analysis.AllocCallback $bm
    sed -i 's/\sThese\sstatistics/\n# Allocation statistics generated using version '$VERSION'\n#\n# These statistics/'g $YML
done
