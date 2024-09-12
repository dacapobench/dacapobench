#!/usr/bin/bash
# 
# This script is for running on the default hardware (Zen4)
# with turbo boost enabled.
# 
mkdir -p ./data
running runbms ./data ./boost.yml -s 2 -p "dacapo-boost"
