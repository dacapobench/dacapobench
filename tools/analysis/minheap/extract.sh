#!/usr/bin/bash
#
# Etract min heap results and dump them in yml files
#

LINES=5 # number of benchmarks + 1 (used to parse file)
BASE=`pwd`/../../../..
OUT=stats-minheap.yml
CFGS=("open-jdk-17.s.cp.gc-G1.t-32.f-10.n-1"\
	  "open-jdk-17.sz-small.s.cp.gc-G1.t-1.f-10.n-1"\
	  "open-jdk-17.sz-large.s.cp.gc-G1.t-32.f-10.n-1"\
	  "open-jdk-17.sz-vlarge.s.cp.gc-G1.t-32.f-10.n-1"\
	  "open-jdk-17.s.cp.gc-Parallel.t-32.f-10.n-1"\
	  "open-jdk-17.s.cp.gc-G1.t-1.f-10.n-1"\
	  "open-jdk-17.s.up.gc-G1.t-32.f-10.n-1"\
	  "open-jdk-17.s.cp.gc-G1.t-32.f-10.n-10"\
	  "open-jdk-11.s.cp.gc-G1.t-32.f-10.n-1")

printf -v HEADER '#
# Minimum heap sizes in which benchmark will run to completion, using
# -Xms<x> -Xmx<x> for various x and various configuarations.  Discovered
# by bisection search.  Numbers below are in MB and reflect results of
# three trials.
#'
BMS=$BASE/benchmarks/bms

for bm in `cd $BMS; ls`; do
    CMDS=("java-17-openjdk-amd64 -server -XX:+UseCompressedOops -XX:+UseG1GC -jar dacapo-evaluation-git-744ef415.jar $bm -t 32 -f 10 -n 1"\
          "java-17-openjdk-amd64 -server -XX:+UseCompressedOops -XX:+UseG1GC -jar dacapo-evaluation-git-744ef415.jar $bm -s small -t 32 -f 10 -n 1"\
          "java-17-openjdk-amd64 -server -XX:+UseCompressedOops -XX:+UseG1GC -jar dacapo-evaluation-git-744ef415.jar $bm -s large -t 32 -f 10 -n 1"\
          "java-17-openjdk-amd64 -server -XX:+UseCompressedOops -XX:+UseG1GC -jar dacapo-evaluation-git-744ef415.jar $bm -s vlarge -t 32 -f 10 -n 1"\
	  "java-17-openjdk-amd64 -server -XX:+UseCompressedOops -XX:+UseG1GC -jar dacapo-evaluation-git-744ef415.jar $bm -t 1 -f 10 -n 1"\
	  "java-17-openjdk-amd64 -server -XX:-UseCompressedOops -XX:+UseG1GC -jar dacapo-evaluation-git-744ef415.jar $bm -t 32 -f 10 -n 1"\
	  "java-17-openjdk-amd64 -server -XX:+UseCompressedOops -XX:+UseParallelGC -jar dacapo-evaluation-git-744ef415.jar $bm -t 32 -f 10 -n 1"\
	  "java-17-openjdk-amd64 -server -XX:+UseCompressedOops -XX:+UseG1GC -jar dacapo-evaluation-git-744ef415.jar $bm -t 32 -f 10 -n 1"\
	  "java-17-openjdk-amd64 -server -XX:+UseCompressedOops -XX:+UseG1GC -jar dacapo-evaluation-git-744ef415.jar $bm -t 32 -f 10 -n 10"\
	  "java-11-openjdk-amd64 -server -XX:+UseCompressedOops -XX:+UseG1GC -jar dacapo-evaluation-git-744ef415.jar $bm -t 32 -f 10 -n 1")
    if [ "$bm" != "common.xml" ]; then
	echo $bm
	out=$BMS/$bm/$OUT
	echo "$HEADER" > $out
	for i in ${!CFGS[@]}; do
	    cfg=${CFGS[$i]}
	    line="$cfg: [ "
	    sz=$(echo $cfg | cut -d'.' -f2 | cut -c1-2)
	    if [ "$sz" == "sz" ]; then # extra "sz" component
		bc=$(echo $cfg | cut -d'.' -f1-7)
		n=$(echo $cfg | cut -d'.' -f8 | cut -c3-10)
	    else
		bc=$(echo $cfg | cut -d'.' -f1-6)
		n=$(echo $cfg | cut -d'.' -f7 | cut -c3-10)
	    fi
	    for j in 0 1 2; do
		v=$(grep -A$LINES $bc ./minheap-out-$n-$j.yml | grep "$bm:" | cut -d' ' -f6 )
		line="$line$v"
		if [ "$j" != "2" ]; then
		    line="$line, "
		else
		    line="$line ]"
		    len=${#line}
		    add=$( expr 70 - $len)
		    for k in `seq 1 $add`; do
			line="$line ";
		    done
		    line="$line # ${CMDS[$i]}"
		    echo "$line" >> $out
		fi
	    done
	done
    fi
done
