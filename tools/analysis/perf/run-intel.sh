#!/usr/bin/bash
# 
# This script is for running on Intel
# 
mkdir -p ./data
running runbms ./data ./baseline_intel.yml -s 2 -p "dacapo-intel"
running runbms ./data ./singlecore_intel.yml -s 2 -p "dacapo-intel"
