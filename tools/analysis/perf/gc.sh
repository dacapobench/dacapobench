#!/bin/bash
#
# Create stats-gc.yml for a benchmark (to stdout)
#
# Uses parsegclog.py to parse a DaCapo log file and produce
# yml.
#
bm=$1    # benchmark name
log=$2   # name of the root directory containing the log files

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

jdk="Temurin-21.0.4+7"
amd="AMD Ryzen 9 7950X 16/32 4.5GHz cores"
intel="Intel Core i9-12900KF Alder Lake 8/16 3.2GHz + 8/8 2.4GHz cores"
arm="Ampere Altra 80 3.0GHz cores"
os="Linux 6.8.0-40"
dacapo="dacapo-evaluation-git-071e5040"

echo "#"
echo "# This data is part of the DaCapo benchmark suite."
echo "#"
echo "# Please refer to https://www.dacapobench.org/ for instructions on how to"
echo "# correctly cite this work."
echo "#"
echo "# This data summarizes GC logs gathered over 10 interations of the benchmark,"
echo "# for different heap sizes.  For each heap size, a list of tuples is given."
echo "# Each tuple presents the start time (sec), the minimum heap size (MB),"
echo "# and the pause time (ms) for the respective GC."
echo "#"
echo "# For example, the tuple "[7.218, 16, 0.94]" reports a GC that started at"
echo "# 7.218s into the program execution, that resulted in a post-GC heap size"
echo "# of 16MB, and that took 0.94ms."
echo "#"
echo "# The heap sizes are reported as mulitples of the minimum heap size for the"
echo "# benchmkark (1.0 means 1.0 x the min heap)."
echo "#"
echo "# These results were gathered using the following DaCapo version:"
echo "#    $dacapo"
echo "#"
echo "# The following JVM was used:"
echo "#    $jdk"
echo "#"
echo "# Results were generated on the following platform:"
echo "#    $amd, $os"
echo "#"
echo "#"


for hf in 2000; do
    echo -n "  $hf: " | sed -e s/000:/.0:/g
    $SCRIPT_DIR/parsegclog.py $log/*-gclog-?ole*/$bm.$hf.*.0/gc.log
done
