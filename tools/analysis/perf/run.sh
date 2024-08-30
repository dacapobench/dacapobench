#!/usr/bin/bash
# 
# This script is for running on the default hardware configuration (Zen4)
# 
# The following jobs need to be run independently, on different hardware
#
# on ARM
#  baseline_arm.yml
#  singlecore_arm.yml
# on Intel
#  baseline_intel.yml
#  singlecore_intel.yml
# on AMD (with turbo-bost settings)
#  boost.yml
# on AMD (with memory speed settings)
#  memory.yml

mkdir -p ./data
running runbms ./data ./baseline.yml -s 10,5,1,7,3,9,8,6,4,2 -p "dacapo-baseline"
running runbms ./data ./compiler.yml -s 2 -p "dacapo-compiler"
running runbms ./data ./gclog.yml -s 2 -p "dacapo-gclog" -i 1
running runbms ./data ./llc.yml -s 2 -p "dacapo-llc"
running runbms ./data ./tma.yml -s 2 -p "dacapo-tma"
running runbms ./data ./tma_be.yml -s 2 -p "dacapo-tma-be"
running runbms ./data ./variants.yml -s 2 -p "dacapo-variants"
running runbms ./data ./baseline_perf.yml -s 10,5,1,7,3,9,8,6,4,2 -p "dacapo-baseline-perf"
running runbms ./data ./variants_perf.yml -s 2 -p "dacapo-variants-perf"
