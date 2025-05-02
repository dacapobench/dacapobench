#!/bin/bash

JAVA_FLAGS="-Djava.security.manager=allow -Dsys.ai.h2o.debug.allowJavaVersions=21"

if [ $# -ne 1 ]
  then
    echo "Usage: $0 <dacapo basename>"
    exit 1
fi

BASE="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

TMP_DIR=`mktemp -d -p "$BASE"`

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

cd $TMP_DIR

# for size in huge large default small; do
for size in default small; do
  MARKER=$BASE/timestamp-$size
  USED=$BASE/files-dat-used-$size.txt
  ALL=$BASE/files-dat-all-$size.txt
  rm -f $USED
  JARS=$BASE/files-jar-used-$size.txt
  rm -f $JARS

  # the find command itself affects access time
  #  find $DACAPO_BASE/dat -type f > $ALL
  date
  touch $MARKER
  echo "Sleeping for 1 hour"
  sleep 3600

  #
  # Run the benchmarks
  #
  for bm in `java -jar $JAR -l 2>/dev/null`; do
  # for bm in lusearch; do
    LOG=$bm-$size.log
    echo $bm
    java -verbose:class $JAVA_FLAGS  -jar $JAR -s $size -n 1 $bm > $LOG >&1
    grep PASS $LOG
    grep FAIL $LOG
    grep java.lang.Error $LOG
    grep $DACAPO_BASE $LOG | grep  "/jar/" >> $JARS
  done

  date
  echo "Sleeping for 1 hour"
  sleep 3600
  echo "find $DACAPO_BASE/dat -type f -anewer $MARKER"
  echo "Output to $USED"
  find $DACAPO_BASE/dat -type f -anewer $MARKER > $USED
done