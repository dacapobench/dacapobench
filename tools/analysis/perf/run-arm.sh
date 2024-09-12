#!/usr/bin/bash
# 
# This script is for running on ARM
# 
mkdir -p ./data
running runbms ./data ./baseline_arm.yml -s 2 -p "dacapo-arm"
running runbms ./data ./singlecore_arm.yml -s 2 -p "dacapo-arm"