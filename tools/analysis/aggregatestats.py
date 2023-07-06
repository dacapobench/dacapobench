#!/usr/bin/env python3
#
# Create summary nominal statistics for a given benchmark
#
# The script uses the various stats-*.yml statistics files
# to derive summary statistics, which printed to std-out.
#
# Author: Steve Blackburn 2023
#
import yaml
import sys
import getopt
import os.path
import math
import statistics

alloc = {}       # allocation stats
bytecode = {}    # bytecode execution stats
minheap = {}     # min heap size stats
perf = {}        # performance stats

nom = {}         # nominal stats
desc = {}        # description of nominal stats

verbose = False

def usage(errno):
    print ('usage: ',sys.argv[0], '-b <path to benchmark>')
    sys.exit(errno)

def loadyml(bmpath):
    global alloc
    global bytecode
    global minheap
    global perf

    yml = bmpath + '/stats-alloc.yml'
    if os.path.exists(yml):
        with open(yml, 'r') as y:
            alloc = yaml.load(y, Loader=yaml.Loader)

    yml = bmpath + '/stats-bytecode.yml'
    if os.path.exists(yml):
        with open(yml, 'r') as y:
            bytecode = yaml.load(y, Loader=yaml.Loader)

    yml = bmpath + '/stats-minheap.yml'
    if os.path.exists(yml):
        with open(yml, 'r') as y:
            minheap = yaml.load(y, Loader=yaml.Loader)

    yml = bmpath + '/stats-perf.yml'
    if os.path.exists(yml):
        with open(yml, 'r') as y:
            perf = yaml.load(y, Loader=yaml.Loader)
def aggregate(results):
    std = []
    mean = []
    mini = []

    invocations = len(results)
    iterations = len(results[0])

    for itr in range(0, iterations):
        vals = []
        for inv in range(0, invocations):
            vals.append(results[inv][itr])
        std.append(statistics.stdev(vals))
        mean.append(statistics.mean(vals))
        mini.append(min(vals))

    return std, mean, mini;

def getperfstats():
    global perf

    if perf is None:
        return None, None, None, None, None, None;

    vm_base = 'open-jdk-11.s.cp.gc-G1'
    vm = vm_base+'.t-32'
    vm_one = vm_base+'.taskset-0'
    par_hf = 2000
    par_threads = 8

    std = {}
    mean = {}
    mini = {}
    best = None
    best_hf = None
    tightest_hf = 20000
    for hf in perf[vm].keys():
        if perf[vm][hf]:
            if hf < tightest_hf:
                tightest_hf = hf
            std[hf], mean[hf], mini[hf] = aggregate(perf[vm][hf])
            if not best:
                best = min(mean[hf])
                best_hf = hf
            else:
                best = min(best, min(mean[hf]))
                if best == min(mean[hf]):
                    best_hf = hf

    # nominal perf (ms -> sec)
    np = math.ceil(best/1000)

    # heap sensitivity
    tight = min(mean[tightest_hf])
    hs = int(100*(tight-best)/best)
    if (hs < 0):
        hs = 0

    # warmup (iteration by which the mean is within 2.5% of the best mean)
    tgt = 1.025*best
    wu = 0
    while mean[best_hf][wu] >= tgt:
        wu = wu + 1

    # standard deviation (as a percentage) among invocations at peak performance
    st = int(100*std[best_hf][wu]/mean[best_hf][wu])

    # parallel efficiency (speedup versus ideal speedup)
    xone = perf[vm][par_hf]
    one = perf[vm_one][par_hf]
    std_one, mean_one, mini_one  = aggregate(one)
    pa = int(100*mean_one[wu]/(par_threads*mean[par_hf][wu]))


    # pa = 0 # Fix

    # vm = 'open-jdk-17.ms.s.hotspot_gc-Parallel.t-12'
    # if (not vm in perf):
    #     vm = 'open-jdk-11.ms.s.hotspot_gc-Parallel.t-12'
    # elif (not '4000' in perf[vm]):
    #     vm = 'open-jdk-11.ms.s.hotspot_gc-Parallel.t-12'

    # ap = perf[vm][4000][9]
    # # nominal perf (ms -> sec)
    # np = math.ceil(ap/1000)

    # # heap sensitivity
    # hs = int(100*(perf[vm][2000][9]-perf[vm][4000][9])/perf[vm][4000][9])
    # if (hs < 0):
    #     hs = 0

    # # warmup
    # tgt = 1.05*perf[vm][4000][9]
    # wu = 0
    # while perf[vm][4000][wu] > tgt:
    #     wu = wu + 1

    return best, np, hs, wu, st, pa;

def objectsizehisto():
    if alloc is None:
        return None;
    histo = {}
    total = 0
    for s in alloc['objects-by-size']:
        total = total + alloc['objects-by-size'][s]
        histo[s] = total
    
    return histo, total;

def getpercentile(histo, total, percentile):
    global alloc

    target = total * percentile

    t = 0
    for s in alloc['objects-by-size']:
        t = t + alloc['objects-by-size'][s]
        if t >= target:
            return s;

    return None; # should not reach here

def nominal():
    ap, np, hs, wu, st, pa = getperfstats()


    if (not alloc is None):
        histo, total = objectsizehisto()

        nom['OSM'] = int(getpercentile(histo, total, 0.5))
        desc['OSM'] = 'nominal median object size (bytes)'

        nom['OSS'] = int(getpercentile(histo, total, 0.1))
        desc['OSS'] = 'nominal 10-percentile object size (bytes)'

        nom['OSL'] = int(getpercentile(histo, total, 0.9))
        desc['OSL'] = 'nominal 90-percentile object size (bytes)'

        nom['OSA'] = int(alloc['bytes-allocated']/alloc['objects-allocated'])
        desc['OSA'] = 'nominal average object size (bytes)'
        
        nom['ALR'] = int(alloc['bytes-allocated']/(1000*ap))
        desc['ALR'] = 'nominal allocation rate (bytes / usec)'

        nom['MTO'] = int(alloc['bytes-allocated']/(1024*1024*(max(minheap['open-jdk-11.s.cp.gc-G1.t-32.f-10.n-1']))))
        desc['MTO'] = 'nominal memory turnover (total alloc bytes / min heap bytes)'

    nom['HSS'] = hs
    desc['HSS'] = 'nominal heap size sensitivity (slowdown with tight heap, as a percentage)'

    nom['MHC'] = max(minheap['open-jdk-11.s.cp.gc-G1.t-32.f-10.n-1'])
    desc['MHC'] = 'nominal minimum heap size (MB) (with compressed pointers)'

    nom['MHU'] = max(minheap['open-jdk-11.s.up.gc-G1.t-32.f-10.n-1'])
    desc['MHU'] = 'nominal minimum heap size (MB) without compressed pointers'

    one = max(minheap['open-jdk-11.s.cp.gc-G1.t-32.f-10.n-1'])
    ten = max(minheap['open-jdk-11.s.cp.gc-G1.t-32.f-10.n-10'])
    if (not ten is None):
        leakage = int(100*((ten/one)-1))
        if (leakage < 0):
            leakage = 0
        nom['LKG'] = leakage
        desc['LKG'] = 'nominal percent memory leakage (10 iterations / 1 iterations) ('+str(ten)+'/'+str(one)+')'

    nom['EXT'] = np
    desc['EXT'] = 'nominal execution time (sec)'

    nom['WRM'] = wu
    desc['WRM'] = 'nominal iterations to warm up to within 2.5% of best'

    nom['PAR'] = pa
    desc['PAR'] = 'nominal parallel efficiency (speedup as percentage of ideal speedup for 8 threads)'

    nom['STD'] = st
    desc['STD'] = 'nominal standard deviation among invocations at peak performance (as percentage of performance)'

    if (not bytecode is None):
        nom['BCU'] = int(bytecode['executed-bytecodes-unique']/1000)
        desc['BCU'] = 'nominal thousands of unique bytecodes executed'

        nom['BCF'] = int(1000*bytecode['executed-bytecodes-p9999']/bytecode['executed-bytecodes'])
        desc['BCF'] = 'nominal execution focus / dominance of hot code'

        nom['BCC'] = int(bytecode['executed-calls-unique']/1000)
        desc['BCC'] = 'nominal thousands of unique function calls'

        nom['PFR'] = int(bytecode['opcodes']['putfield']/(1000*ap))
        desc['PFR'] = 'nominal putfield per usec'
    
        nom['GFR'] = int(bytecode['opcodes']['getfield']/(1000*ap))
        desc['GFR'] = 'nominal getfield per usec'

        nom['AAR'] = int(bytecode['opcodes']['aastore']/(1000*ap))
        desc['AAR'] = 'nominal aastore per usec'

        nom['ALR'] = int(bytecode['opcodes']['aaload']/(1000*ap))
        desc['ALR'] = 'nominal aaload per usec'



    print("stats:")
    for x in sorted(nom):
        print("  "+x+": "+str(nom[x])+"\t\t# "+desc[x])
  
    # scalability (1 thread v N threads)
    # heap leakage (minheap 1 it v minheap 10 it)
    # heap threads (minheap 1 thread v minheap N threads)
    # heap turnover (alloc / minheap)
    # median object size
    # code intensity (hotspot)
    # 

def main(argv):
    bmpath = None

    try:
        opts, args = getopt.getopt(argv, "hvb:")
    except getopt.GetoptError as e:
        print ('ERROR: Incorrect arguments'.format(e.errno, e.sterror))
        usage(2)
    for opt,arg in opts:
        if opt == '-h':
            usage(0)
        elif opt == '-v':
            verbose = True
        elif opt == '-b':
            bmpath = arg
    
    if bmpath is None or not os.path.exists(bmpath):
        print ('ERROR: You must specify a valid path to a benchmark')
        usage(2)
    else:
        loadyml(bmpath)
        nominal()

    exit(0)

if __name__ == "__main__":
    main(sys.argv[1:])