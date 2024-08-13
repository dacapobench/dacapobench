This directory contains scripts for establishing minheap statistics.

NOTE: This is a time-consuming process, taking in the order of a week of compute time.

The goal is to establish for each benchmark, for each of a series of configurations, the minimum heap size in which that benchmark will run with those configurations.  The scripts independently establish this number five times, so the minheap statistics consist of five values reflecting five trials, which in turn reflects the stability of the measurel.

### Using the scripts

1. Invoke `run.sh`:
   - This first creates an output directory, `out` in the current working directory
   - NOTE: if there already exists an output directory and it already contains results, the scripts may do nothing due to the behavior of the running scripts which will only run the benchmark if there does not already exist a result.
   - The script then generates text files containing the 'dryrun' output for the commands about to be run.  These files are later used to document the precise commands used to generate the reults.
   - The script then generates five separate sets of results.
   - Due to a quirk in the running scripts, the number of iterations of the benchmark cannot be expressed in a config, thus there are three yml config files, one for single iteration, on for 5 iterations and one for 10 iterations.
   - The running script uses these config files to determine what to run.
2. Once complete, run `scrapeminheaps.py`:
   - This will take the above output and generate separte yml files for each benchmark summarizing the results.
