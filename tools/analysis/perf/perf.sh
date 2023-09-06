#!/bin/bash
#
# Create stats-perf.yml for a benchmark (to stdout)
#
# Uses perflogtoyml.py to parse a DaCapo log file and produce
# yml.
#
bm=$1    # benchmark name
log=$2   # the name of the log directory containing the log files
xlog=$3  # the name of the log directory containing the log files for the extra runs

dacapo=744ef415
hfacs="1000 2000 3000 4000 5000 6000 7000 8000 9000 10000"
hardware="AMD Ryzen 9 7950X 16/32 cores."
os="Linux 5.15.0."

echo "#"
echo "# Execution times in msec for various configurations at various heap"
echo "# factors. The heap factor is 1000 * (heap size / minimum heap size)."
echo "# So a heap factor of 1000 is the smallest heap the workload will run"
echo "# in; a heap factor of 2000 is twice the minimum heap size, etc."
echo "#"
echo "# Each row represents one invocation of the benchmark, showing the"
echo "# times for each iteration within that invocation."
echo "#"
echo "# Multiple invocations are reported.  These reflect the distribution"
echo "# (variance) of the workload across invocations."
echo "#"
echo "# These results were gathered on the following hardware:"
echo "#"
echo "# $hardware"
echo "# $os"
echo "#"

# main perf config
d=log/$log
cfg="open-jdk-17.s.cp.gc-G1.t-32"
echo "$cfg:"
for hf in $hfacs; do
    echo "  $hf:"
    zcat $d/$bm\.$hf\.*.$cfg.f-*.dacapo-*.log.gz  | ./perflogtoyml.py -i 4
done

# extra configs
d=log/$xlog

for cfg in open-jdk-17.s.cp.gc-G1.taskset-0 open-jdk-17.s.cp.gc-Serial open-jdk-17.s.cp.gc-Parallel open-jdk-17.s.cp.gc-Z open-jdk-17.s.cp.gc-Shenandoah ; do
    echo "$cfg:"
    for hf in 2000; do
	echo "  $hf:"
	zcat $d/$bm\.$hf\.*.$cfg.f-*.dacapo-*.log.gz  | ./perflogtoyml.py -i 4
    done
done
