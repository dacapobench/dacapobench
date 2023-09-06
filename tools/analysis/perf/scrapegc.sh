#!/usr/bin/bash
#
# gather all the GC stats for all benchmarks into yml files

DIR=log/mole-2023-08-30-Wed-115514

cfg=open-jdk-17.s.cp.gc-G1.t-32.f-40
for bm in `(cd $DIR; ls | grep 2000 | cut -d '.' -f1 | sort | uniq)`; do
    echo $bm
    out=../../../benchmarks/bms/$bm/stats-gc.yml
    cp gc-stats-hdr.txt $out
    for hf in 2; do
	echo -n "$hf.0: " >> $out
	./parsegclog.py $DIR/$bm.$hf???.*$cfg*.0/gc.log >> $out
    done
done

