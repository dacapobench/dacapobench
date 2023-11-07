#!/usr/bin/bash
#
# gather all the GC stats for all benchmarks into yml files

# DIR=log/mole-2023-08-30-Wed-115514
DIR=$HOME/dacapo/dacapo-pldi-2024/data/dacapo-pldi-2024-gclog-mole-2023-11-07-Tue-081515

for bm in `(cd $DIR; ls | grep 2000 | cut -d '.' -f1 | sort | uniq)`; do
    echo $bm
    out=$HOME/devel/dacapo/dacapobench/benchmarks/bms/$bm/stats-gc.yml
    cp gc-stats-hdr.txt $out
    for hf in 2; do
	echo -n "$hf.0: " >> $out
	./parsegclog.py $DIR/$bm*.0/gc.log >> $out
    done
done

