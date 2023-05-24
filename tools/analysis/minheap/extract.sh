#!/usr/bin/bash
#
# Etract min heap results and dump them in yml files
#

LINES=24 # number of benchmarks plus header (needed to parse output)
BASE=`pwd`/../../..
OUT=stats-minheap.yml
CFGS=("open-jdk-11.s.cp.gc-G1.t-32.f-10.n-1" "open-jdk-11.s.cp.gc-G1.t-1.f-10.n-1" "open-jdk-11.s.up.gc-G1.t-32.f-10.n-1" "open-jdk-11.s.cp.gc-Parallel.t-32.f-10.n-1" "open-jdk-17.s.cp.gc-G1.t-32.f-10.n-1" "open-jdk-11.s.cp.gc-G1.t-32.f-10.n-10")

printf -v HEADER '#
# Minimum heap sizes in which benchmark will run to completion, using
# -Xms<x> -Xmx<x> for various x and various configuarations.  Discovered
# by bisection search.  Numbers below are in MB and reflect results of
# three trials.
#'
BMS=$BASE/benchmarks/bms

for bm in `cd $BMS; ls`; do
    CMDS=("java-11-openjdk-amd64 -server -XX:+UseCompressedOops -XX:+UseG1GC -jar dacapo-evaluation-git-78bcea0f.jar $bm -t 32 -f 10 -n 1" "java-11-openjdk-amd64 -server -XX:+UseCompressedOops -XX:+UseG1GC -jar dacapo-evaluation-git-78bcea0f.jar $bm -t 1 -f 10 -n 1" "java-11-openjdk-amd64 -server -XX:-UseCompressedOops -XX:+UseG1GC -jar dacapo-evaluation-git-78bcea0f.jar $bm -t 32 -f 10 -n 1" "java-11-openjdk-amd64 -server -XX:+UseCompressedOops -XX:+UseParallelGC -jar dacapo-evaluation-git-78bcea0f.jar $bm -t 32 -f 10 -n 1" "java-17-openjdk-amd64 -server -XX:+UseCompressedOops -XX:+UseG1GC -jar dacapo-evaluation-git-78bcea0f.jar $bm -t 32 -f 10 -n 1" "java-11-openjdk-amd64 -server -XX:+UseCompressedOops -XX:+UseG1GC -jar dacapo-evaluation-git-0d047f55.jar $bm -t 32 -f 10 -n 10")
    if [ "$bm" != "common.xml" ]; then
	echo $bm
	out=$BMS/$bm/$OUT
	echo "$HEADER" > $out
	for i in ${!CFGS[@]}; do
	    cfg=${CFGS[$i]}
	    line="$cfg: [ "
            bc=$(echo $cfg | cut -d'.' -f1-6)
	    n=$(echo $cfg | cut -d'.' -f7 | cut -c3-10)
	    for j in 0 1 2; do
		v=$(grep -A24 $bc ./minheap-out-$n-$j.yml | grep "$bm:" | cut -d' ' -f6 )
		line="$line$v"
		if [ "$j" != "2" ]; then
		    line="$line, "
		else
		    line="$line ]"
		    len=${#line}
		    add=$( expr 64 - $len)
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
