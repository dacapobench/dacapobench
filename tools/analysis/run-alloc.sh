#!/usr/bin/bash

VERSION=evaluation-git-6b469af0
BASEDIR=$(cd ../../benchmarks; pwd)
JAVA=/usr/lib/jvm/temurin-21-jdk-amd64/bin/java # > 11
JAR=$BASEDIR/dacapo-$VERSION.jar

for bm in `$JAVA -jar $BASEDIR/dacapo-$VERSION.jar -l 2>/dev/null`; do
    $JAVA -Ddacapo.alloc.yml=$BASEDIR/bms/$bm/stats-alloc.yml -Djava.security.manager=allow -javaagent:$BASEDIR/dacapo-$VERSION/jar/java-allocation-instrumenter-3.3.4.jar -jar $BASEDIR/dacapo-$VERSION.jar -callback org.dacapo.analysis.AllocCallback $bm
done