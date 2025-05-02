#!/bin/bash
#
# This script takes a dacapo zip file as an argument and uses
# the non-minimal-files.txt file to create a new minimal zip file.
#
# The new zip will have the suffix "minimal" and will include 
# similarly named jar and base directories.
#
# More specifically, the script will:
#   - remove the files listed in non-minimal-files.txt,
#   - update the jar to:
#     - remove the md5 entries for each of the removed files
#     - remove "large" and "huge" configs from the cfg metadata of each of the affected benchmarks
#     - update the mainfest to add the "mimimal" suffix
#   - create a new zip with the minimized base directory and new jar
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

function cleanup {      
  rm -rf "$TMP_DIR"
  echo "Deleted temp directory $TMP_DIR"
}
trap cleanup EXIT

#
# Check command line argument
#
bigzip=`realpath $1`
if [ -f "$bigzip"  ]; then
  VERSION=`basename $bigzip .zip`
  echo "Found $bigzip, for $VERSION"
else
  echo "Could not find zipfile '$bigzip'"
  exit 1
fi

cd $TMP_DIR

#
# Unzip the full distro
#
unzip $bigzip

#
# Unzip the jar into a jar directory
#
JAR_DIR="$TMP_DIR/jar"
mkdir -p $JAR_DIR
cd $JAR_DIR
unzip ../$VERSION.jar
cd $TMP_DIR

#
# Delete each non-minimal file and remove its md5 sum
#
for f in `cat $BASE/non-minimal-files.txt`; do
    bm=`echo $f | cut -d '/' -f2`
    chmod -f u+w $TMP_DIR/$VERSION/$f
    rm -f $TMP_DIR/$VERSION/$f
    grep -v $f $JAR_DIR/META-INF/md5/$bm.MD5 > tmp.MD5
    mv tmp.MD5 $JAR_DIR/META-INF/md5/$bm.MD5
done

#
# Prune large and huge configs out of each affected benchmark's cnf file
#
bms=`cat $BASE/non-minimal-files.txt | cut -d '/' -f 2 | sort | uniq`
list=""
for bm in $bms; do
    list=${list}" $bm"
    cnf=$JAR_DIR/META-INF/cnf/$bm.cnf
    lg=$((`grep -n "size large" $cnf | cut -d ':' -f 1`-1))
    de=$((`grep -n "description" $cnf | cut -d ':' -f 1`))
    p='p'
    e1="1,$lg$p"
    e2="$de,1000p"
    sed -i -n -e $e1 -e $e2 $cnf
done

#
# Update the MANIFEST
#
match=`grep Implementation-Version $JAR_DIR/META-INF/MANIFEST.MF | sed -e 's/[[:space:]]*$//'`
suf="-minimal"
sed -i "s/${match}/${match}${suf}\nTrimmed-Benchmarks:${list}/" $JAR_DIR/META-INF/MANIFEST.MF

#
# Create new jar
#
cd $JAR_DIR
MIN_VERSION=$VERSION$suf
zip -r ../$MIN_VERSION.jar .
cd $TMP_DIR

#
#  Move main dir and create zip
#
mv $VERSION $MIN_VERSION
zip -r $BASE/$MIN_VERSION.zip $MIN_VERSION.jar $MIN_VERSION