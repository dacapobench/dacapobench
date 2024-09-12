#!/usr/bin/bash
# 
# This script is for running on the default hardware (Zen4)
# with memory speed reduced.
# 
mkdir -p ./data
running runbms ./data ./memory.yml -s 2 -p "dacapo-memory"