#!/bin/bash
#
# This script takes a dacapo zip file as an argument and uses
# the unneeded-files.txt and unneeded-files-aggressive.txt files
# to create two new minimal zip files.
#
# - unneeded-files.txt is a list of files that no config needs
# - unneeded-files-aggressive.txt is a list of large files only need by some large configs
#
# The original zip will be replaced with an equivalent one that has unneeded
# files removed.
#
# A new zip will also be created, with the suffix "minimal" and will include 
# similarly named jar and base directories.  This second zip will not work with 
# some large configs.
#
# More specifically, the script will:
#   - remove all files listed in unneeded-files.txt,
#   - update the jar to:
#     - remove the md5 entries for each of the removed files
#   - create a new zip with the minimized base directory and new jar
#
# It will then repeat the above, but more agressively removing
# large files that are only used by some large configs:
#   - remove the files listed in unneeded-files-aggressive.txt,
#   - update the jar to:
#     - remove the md5 entries for each of the removed files
#     - update the mainfest to add the "mimimal" suffix
#     - remove "large" and "huge" configs from the cfg metadata of each of the affected benchmarks
#  - create a new zip with the minimized base directory and new jar
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
# Unzip the main jar into a jar directory
#
JAR_DIR="$TMP_DIR/jar"
mkdir -p $JAR_DIR
cd $JAR_DIR
unzip ../$VERSION.jar
cd $TMP_DIR

#
# Delete each unneeded file and remove its md5 sum
#
for f in `cat $BASE/unneeded-files.txt`; do
    chmod -f u+w $TMP_DIR/$VERSION/$f
    rm -f $TMP_DIR/$VERSION/$f
    cd $JAR_DIR
    for m in `grep -l $f META-INF/md5/*`; do
      grep -v $f $m > tmp.MD5
      mv tmp.MD5 $m
    done
    cd $TMP_DIR
done

#
# Create new jar
#
cd $JAR_DIR
rm -f ../$VERSION.jar
zip -r ../$VERSION.jar .
cd $TMP_DIR

#
#  Create zip
#
rm -f $BASE/$VERSION.zip
zip -r $BASE/$VERSION.zip $VERSION.jar $VERSION


# 
# Now more agressively remove files, which will affect some configs
#

#
# Delete each unneeded file and remove its md5 sum
#
for f in `cat $BASE/unneeded-files-aggressive.txt`; do
    bm=`echo $f | cut -d '/' -f2`
    chmod -f u+w $TMP_DIR/$VERSION/$f
    rm -f $TMP_DIR/$VERSION/$f
    grep -v $f $JAR_DIR/META-INF/md5/$bm.MD5 > tmp.MD5
    mv tmp.MD5 $JAR_DIR/META-INF/md5/$bm.MD5
done

#
# More aggressively prune large and huge configs out of each affected benchmark's cnf file
#
bms=`cat $BASE/unneeded-files-aggressive.txt | cut -d '/' -f 2 | sort | uniq`
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