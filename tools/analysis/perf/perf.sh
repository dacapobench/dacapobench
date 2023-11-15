#!/bin/bash
#
# Create stats-perf.yml for a benchmark (to stdout)
#
# Uses perflogtoyml.py to parse a DaCapo log file and produce
# yml.
#
bm=$1    # benchmark name
log=$2   # name of the root directory containing the log files

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

hfacs="1000 2000 3000 4000 5000 6000 7000 8000 9000 10000"
hardware="AMD Ryzen 9 7950X 16/32 cores."
os="Linux 6.2."

echo "#"
echo "# Execution times in msec for various configurations at various heap"
echo "# sizes. The heap size is expressed as a multiple of the minimum heap"
echo "# size. So a heap size of 1.0 is the smallest heap the workload will run"
echo "# in when using the default G1 configuration; a heap size of 2.0 is twice"
echo "# the minimum heap size, etc."
echo "#"
echo "# Each row represents one invocation of the benchmark, showing the"
echo "# times for each iteration within that invocation."
echo "#"
echo "# Multiple invocations are reported.  These reflect the distribution"
echo "# (variance) of the workload across invocations.  Missing data reflects"
echo "# the workload failing to complete with that configuration."
echo "#"
echo "# These results were gathered on the following hardware:"
echo "#"
echo "# $hardware"
echo "# $os"
echo "#"

# main perf config
cfg="open-jdk-21.server.G1.t-32"
echo "$cfg:"
for hf in $hfacs; do
    echo "  $hf:" | sed -e s/000:/.0:/g
    zcat $log/*baseline-vole*/$bm.$hf.*.log.gz  | $SCRIPT_DIR/perflogtoyml.py -i 4
done

# extra configs

# collectors
hf=2000
for cfg in G1.taskset-0 Parallel.mu_threads-32 Serial.mu_threads-32 Shenandoah.mu_threads-32 Z.mu_threads-32 Z.zgc_gen.mu_threads-32; do
    echo "open-jdk-21.server.$cfg:" | sed -e s/mu_threads-/t-/g | sed -e s/Z.zgc_gen/Zgen/g
    echo "  $hf:" | sed -e s/000:/.0:/g
    zcat $log/*variants-?ole*/$bm.$hf.*-$cfg.*.log.gz  | $SCRIPT_DIR/perflogtoyml.py -i 4
done

# interpreter
echo "open-jdk-21.server.G1.interpreter.t-32:"
echo "  $hf:" | sed -e s/000:/.0:/g
zcat $log/*2024-interpreter-?ole*/$bm.$hf.*dacapo*.log.gz  | $SCRIPT_DIR/perflogtoyml.py -i 4

# compilers
for cfg in c1.comp c1 c2.comp c2; do
    echo "open-jdk-21.server.G1.$cfg.t-32:"
    echo "  $hf:" | sed -e s/000:/.0:/g
    zcat $log/*compiler-?ole*/$bm.$hf.*.$cfg.dacapo*.log.gz  | $SCRIPT_DIR/perflogtoyml.py -i 4
done

# cache
for cfg in resctrl-0001 resctrl-ffff; do
    echo "open-jdk-21.server.G1.$cfg.t-32:"
    echo "  $hf:" | sed -e s/000:/.0:/g
    zcat $log/*2024-llc-?ole*/$bm.$hf.*.$cfg.dacapo*.log.gz  | $SCRIPT_DIR/perflogtoyml.py -i 4
done

# memory
echo "open-jdk-21.server.G1.slow-memory.t-32:"
echo "  $hf:" | sed -e s/000:/.0:/g
zcat $log/*2024-memory-?ole*/$bm.$hf.*dacapo*.log.gz  | $SCRIPT_DIR/perflogtoyml.py -i 4

# turbo
echo "open-jdk-21.server.G1.turbo-boost.t-32:"
echo "  $hf:" | sed -e s/000:/.0:/g
zcat $log/*2024-boost-?ole*/$bm.$hf.*dacapo*.log.gz  | $SCRIPT_DIR/perflogtoyml.py -i 4

#for cfg in open-jdk-17.s.cp.gc-G1.taskset-0 open-jdk-17.s.cp.gc-Serial open-jdk-17.s.cp.gc-Parallel open-jdk-17.s.cp.gc-Z open-jdk-17.s.cp.gc-Shenandoah ; do
#    echo "$cfg:"
#    for hf in 2000; do
#	echo "  $hf:"
#	zcat $d/$bm\.$hf\.*.$cfg.f-*.dacapo-*.log.gz  | ./perflogtoyml.py -i 4
#    done
#done
