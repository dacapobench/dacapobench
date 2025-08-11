#!/bin/bash
#
# Find all jars that are actually used
#
JAVA_FLAGS="-Djava.security.manager=allow -Dsys.ai.h2o.debug.allowJavaVersions=21"

if [ $# -ne 1 ]
  then
    echo "Usage: $0 <dacapo basename>"
    exit 1
fi

BASE="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

function cleanup {      
#  rm -rf "$TMP_DIR"
  echo "Deleted temp directory $TMP_DIR"
}
trap cleanup EXIT

DACAPO_BASE=`realpath $1`
JAR=$DACAPO_BASE".jar"

if [ -f "$JAR"  ]; then
  VERSION=`basename $JAR .jar`
  echo "Found $JAR, for $VERSION"
else
  echo "Could not find jar '$JAR'"
  exit 1
fi

JAVA_VERSION=`java --version | head -1`

echo "Using Java version: $JAVA_VERSION"


# cd $TMP_DIR

JARS=$BASE/used-jars.txt
rm -f $JARS

#
# Run the benchmarks
#
for bm in `java -jar $JAR -l 2>/dev/null`; do
  echo $bm
  LOG=$bm-jar.log 
  java -verbose:class $JAVA_FLAGS  -jar $JAR -s small -n 2 $bm > $LOG 2>&1
  java -verbose:class $JAVA_FLAGS  -jar $JAR -s default -n 2 $bm >> $LOG 2>&1
  grep PASS $LOG 
  grep FAIL $LOG 
  grep $DACAPO_BASE $LOG | grep  "/jar/" | cut -d ':' -f 3 | sort | uniq >> $JARS
done

