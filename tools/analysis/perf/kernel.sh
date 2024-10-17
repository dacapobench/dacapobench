#!/bin/bash
#
# Create stats-kernel.yml for a benchmark (to stdout)
#
# Uses perflogtoyml.py to parse a DaCapo log file and produce
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
echo "# Total, user and kernel time in milliseconds for the final (10th)"
echo "# iteration of the benchmark when run at 2.0 times the minimum heap"
echo "# size with the default G1 collector on a 32 way machine."
echo "#"
echo "# Total time represents the wall clock time."
echo "# User and kernel time are integrated across all threads, so their sum"
echo "# reflects the total integrated running time."
echo "#"
echo "# For example, the triple '3534, 50975, 13496' indicates a wall clock"
echo "# of 3.532ms, user time of 50.975ms, kernel time of 13.496ms, so the"
echo "# benchmark had 64.471/3.532 = 18.25 utilization on the 32 core machine"
echo "# (about 57% parallel efficiency)."
echo "#"
echo "# Multiple invocations are reported.  These reflect the distribution"
echo "# (variance) of the workload across invocations.  Missing data reflects"
echo "# the workload failing to complete with that configuration."
echo "#"
echo "# These results were gathered using the following DaCapo version:"
echo "#    $dacapo"
echo "#"
echo "# Unless otherwise noted, the following JVM was used:"
echo "#    $jdk"
echo "#"
echo "# Unless otherwise noted, results were generated on the following platform:"
echo "#    $amd, $os"
echo "#"

# main perf config
cfg="open-jdk-21.server.G1.t-32"
echo "$cfg:"
for hf in 2000; do
    echo "  $hf:" | sed -e s/000:/.0:/g
    zcat $log/*baseline-v*/$bm.$hf.*.log.gz  | $SCRIPT_DIR/perflogtoyml.py -k -i 4
done
