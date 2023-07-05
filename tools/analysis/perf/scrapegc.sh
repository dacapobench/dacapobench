#!/usr/bin/bash
#
# gather all the GC stats for all benchmarks into yml files

DIR=log/mole-2023-06-08-Thu-033509

for bm in `(cd $DIR; ls | grep 1000 | cut -d '.' -f1 | sort | uniq)`; do
    echo $bm
    out=../../../benchmarks/bms/$bm/stats-gc.yml
    cp gc-stats-hdr.txt $out
    for i in 1 2; do
	echo -n "$i.0: " >> $out
	./parsegclog.py $DIR/$bm.$i*.0/gc.log >> $out
    done
done

