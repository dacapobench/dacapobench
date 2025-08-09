#!/bin/bash
#
#
if [ $# -ne 1 ]
  then
    echo "Usage: $0 <dacapo zipfile>"
    exit 1
fi

#
# Basic setup and creation of temp directory
#
BASE="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
TMP_DIR=`mktemp -d -p "$BASE"`

JAVA_FLAGS="-Xms1G -Djava.security.manager=allow -Dsys.ai.h2o.debug.allowJavaVersions=21 "

function cleanup {      
  rm -rf "$TMP_DIR"
  echo "Deleted temp directory $TMP_DIR"
}
trap cleanup EXIT

#
# Check command line argument
#
zip=`realpath $1`
if [ -f "$zip"  ]; then
  DACAPO_BASE=`basename $zip .zip`
  JAR=$DACAPO_BASE".jar"
  echo "Found $zip, for $DACAPO_BASE"
else
  echo "Could not find zip file '$zip'"
  exit 1
fi

cd $TMP_DIR

#
# Unzip the full distro
#
unzip $zip

#
# Setup
#
JAVA_VERSION=`java --version | head -1`
echo "Using Java version: $JAVA_VERSION"
echo "Using DaCapo jar: $JAR"
echo "Using temp directory: $TMP_DIR"

#
# Run the benchmarks
#
for size in default small large vlarge; do

  for bm in `java -jar $JAR -l 2>/dev/null`; do
    echo $bm $size
    LOG=$bm-$size.log 
    java $JAVA_FLAGS  -jar $JAR -s $size  $bm > $LOG 2>&1
    grep PASS $LOG
    grep FAIL $LOG 
    grep "No such size" $LOG
    grep java.lang.Error $LOG
  done

done