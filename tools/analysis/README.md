# Analysis Tools for DaCapo

These alaysis tools are used to generate the per-benchmark statistics seen in the `benchmarks/bms/*/stats-*.yml` files.

### MinHeap

**TL;DR**:* Use the `run.sh` script within the `minheap` directory.  Expected running time: O(1 week).

The minheap tools establish the minimum heap sizes in which the benchmarks will run under various configurations.  The results of the minheap analysis are ultimately output to the `benchmarks/bms/<benchmark>/stats-minheap.yml` file.  These in turn contribute to a number of the _nominal statistics_ which appear in the `benchmarks/bms/<benchmark>/stats-nominal.yml` file for each benchmark.

The tools use the [running](https://github.com/anupli/running-ng) scripts' `minheap` functionality, which finds minimum usable heap sizes by bisection search.

The scripts are driven by four config files for the `running` scripts: `minheap-base.yml` which captures the key configurations including the command line flags; `minheap-1.yml` which captures most of the benchmark configurations run (which are all conducted using a single iteration of the respective benchmark); and `minheap-5.yml` and `minheap-10.yml` which run the default workload configuration, but with 5 and 10 iterations of the workload respectively.

The `run.sh` script will execute all of the minheap scripts required to update all of DaCapo.  Note that the complete set of minheap executions may take _around a week_ of compute time.  The script will output results into files in subdirectory `out`.  The script runs every configuration _five_ times, delivering five results, which provide an indication of the stability of the measure.  Some benchmarks are very stable, others will vary from run to run.

The `scrapeminheaps.py` script should be run after the `run.sh` script has completed.  It will parse the generated `.yml` files and the `.txt` files and use them to generate per-benchmark `stats-minheap.yml` files.

Finally, `aggregatestats.py` and `normalizestats.py` need to be run to propogate the minheap stats into the nominal statistics (see below).

### Bytecode Analysis

**TL;DR**:* Use the `run-bytecode.sh` script within this directory. Expected running time: O(1 day).

We use bytecode analysis to generate the `benchmarks/bms/<benchmark>/stats-bytecode.yml` file for each benchmark. The relevant source code is in the `bcc` subdirectory.

The analysis uses a java agent, with the necessary jar pre-built into the DaCapo distribution.  The agent will use a callback, `org.dacapo.BytecodeCallback` which is defined in `benchmarks/harness/src/org/dacapo/analysis`, and compiled into the DaCapo distribution.

Example usage: `java -javaagent:<dacapo_version>/jar/bccagent.jar -Ddacapo.bcc.yml=<output_file> -jar <dacapo_version>.jar -callback org.dacapo.analysis.BytecodeCallback <benchmark>`

* The `-javaagent` flag will activate the bytecode annotator.
* The `-callback` will ensure that the provided callback will be used, which will capture statistics for iterations and output them at the end.
* `-Ddacapo.bcc.yml` specifies the file where the yml will be written (if undefined, output will go to stdout).
* `-Djava.security.manager=allow` is required for the cassandra workload when using more recent JVMs.

The `run-bytecode.sh` script in this directory will run the analysis for all of the bechmarks, updating the respective `stats-bytecode.yml` files for each benchmark.

Finally, `aggregatestats.py` and `normalizestats.py` need to be run to propogate the minheap stats into the nominal statistics (see below).

### Alloc Analysis

**TL;DR**:* Use the `run-alloc.sh` script within this directory. Expected running time: O(1 hr).

We also use bytecode analysis to analyze the allocation behaviors of each workload.  Specifically, we use [this](https://github.com/google/allocation-instrumenter) instrumentation agent.

The `run-alloc.sh` script in this directory will run the analysis for all of the bechmarks, updating the respective `stats-alloc.yml` files for each benchmark.

Finally, `aggregatestats.py` and `normalizestats.py` need to be run to propogate the minheap stats into the nominal statistics (see below).

### Performance

The performance numbers are mostly generated on a single machine in its default configuration using the script `perf/run.sh`.   However, some of the results are generated on ARM and Intel hardware and on the AMD hardware with different configuraitons.  See the note at the top of `perf/run.sh`.

#### Additional prerequisite

You need the distillation tools in `$HOME/dacapo/distillation`:

```
cd $HOME/dacapo
git clone --recursive https://github.com/anupli/dacapo-distillation distillation
cd dacapo-distillation
make -j
```

#### Kernel

The script `kernel.sh` will generate the `stats-kernel.yml` file for a given benchmark.

### Aggregating and Normalizing

Once any of the above analyses have been updated, the `aggregatestats.py` and `normalizestats.py` scripts need to be run to generate the new nominal statistics for each benchmark.  See the usage notes in `aggregatestats.py`.

