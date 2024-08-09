#!/usr/bin/bash

# create the output directory
mkdir out

# use dry-run to document what each of the configs is doing
for i in 1 5 10; do
   running --dry-run minheap minheap-$i.yml foo.txt &> out/minheap-$i-dryrun.txt
done

# actually run the minheap script
for j in 0 1 2 3 4; do
   for i in 1 5 10; do
      running minheap -a 3 minheap-$i.yml out/minheap-out-$i-$j.yml
   done
done
