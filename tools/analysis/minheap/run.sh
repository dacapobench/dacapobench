#!/usr/bin/bash

for i in 0 1 2; do
  running minheap -a 3 minheap.yml minheap-out-10-$i.yml
done
