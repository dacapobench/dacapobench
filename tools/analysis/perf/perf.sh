#!/bin/bash
#
# Create stats-perf.yml for a benchmark (to stdout)
#
# Uses perflogtoyml.py to parse a DaCapo log file and produce
# yml.
#
bm=$1    # benchmark name
log=$2   # the name of the log directory containing the log files

dacapo=78bcea0f
hfacs="1000 2000 3000 4000 5000 6000 7000 8000 9000 10000"
d=log/$log
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

cfg="open-jdk-11.s.cp.gc-G1.t-32"
echo "$cfg:"
for hf in $hfacs; do
    echo "  $hf:"
    zcat $d/$bm\.$hf\.*.$cfg.f-*.dacapo-$dacapo.log.gz  | ./perflogtoyaml.py -i 4
done

cfg="open-jdk-11.s.cp.gc-G1.taskset-0"
echo "$cfg:"
for hf in 2000; do
    echo "  $hf:"
    zcat $d/$bm\.$hf\.*.$cfg.f-*.dacapo-$dacapo.log.gz  | ./perflogtoyaml.py -i 4
done
