#!/usr/bin/bash

VERSION=dacapo-evaluation-git-3745e0c2
BASEDIR=$(cd ../../benchmarks; pwd)
JAVA=/usr/lib/jvm/temurin-21-jdk-amd64/bin/java
JAR=$BASEDIR/$VERSION.jar

for bm in `$JAVA -jar $JAR -l 2>/dev/null`; do
    YML=$BASEDIR/bms/$bm/stats-bytecode.yml
    $JAVA -Ddacapo.bcc.yml=$YML -Djava.security.manager=allow -javaagent:$BASEDIR/$VERSION/jar/bccagent.jar -jar $JAR -callback org.dacapo.analysis.BytecodeCallback $bm
    sed -i 's/\sThese\sstatistics/\n# Bytecode statistics generated using version '$VERSION'\n#\n# These statistics/'g $YML
done
