#!/usr/bin/bash
# 
# This script is for creating yml files once all of the results have been produced
# 
log=$1  # name of the root directory containing the log files (eg perf/data)

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

BASE=$SCRIPT_DIR/../../../benchmarks/bms

for bm in `ls $BASE | grep -v common.xml`; do
    echo $bm
    $SCRIPT_DIR/perf.sh $bm $log > $BASE/$bm/stats-perf.yml
    $SCRIPT_DIR/gc.sh $bm $log > $BASE/$bm/stats-gc.yml
    $SCRIPT_DIR/kernel.sh $bm $log > $BASE/$bm/stats-kernel.yml
    $SCRIPT_DIR/uarch.sh $bm $log > $BASE/$bm/stats-uarch.yml
done
