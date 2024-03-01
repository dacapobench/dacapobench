#!/usr/bin/env python3
#
# Aggregate the various raw statistics.  This needs to be followed by using normalizestats.py
#
# Example usage:
# for bm in `ls ../../benchmarks/bms/ | grep -v common.xml`; do
#   echo $bm
#   ./aggregatestats.py -b ../../benchmarks/bms/$bm > ../../benchmarks/bms/$bm/stats-nominal.yml
# done
# ./normalizestats.py -p ../../benchmarks/bms
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
kernel = {}      # kernel/user stats
gc = {}          # GC stats
uarch = {}       # uarch stats

nom = {}         # nominal stats
desc = {}        # description of nominal stats

verbose = False

def usage(errno):
    print ('usage: ',sys.argv[0], '-b <path to benchmark>')
    sys.exit(errno)

def load_yml(bmpath):
    global alloc
    global bytecode
    global minheap
    global perf
    global kernel
    global gc
    global uarch

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

    yml = bmpath + '/stats-kernel.yml'
    if os.path.exists(yml):
        with open(yml, 'r') as y:
            kernel = yaml.load(y, Loader=yaml.Loader)

    yml = bmpath + '/stats-gc.yml'
    if os.path.exists(yml):
        with open(yml, 'r') as y:
            gc = yaml.load(y, Loader=yaml.Loader)

    yml = bmpath + '/stats-uarch.yml'
    if os.path.exists(yml):
        with open(yml, 'r') as y:
            uarch = yaml.load(y, Loader=yaml.Loader)

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
        if len(vals) == 1:
            std.append(0)
        else:
            std.append(statistics.stdev(vals))
        mean.append(statistics.mean(vals))
        mini.append(min(vals))

    return std, mean, mini;

par_hf = 2.0
par_threads = 32

def get_perf_stats():
    global perf

    if perf is None:
        return None, None, None, None, None, None;

    vm_base = 'open-jdk-21.server.G1'
    vm = vm_base+'.t-32'
    vm_one = vm_base+'.taskset-0'

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
    # hs = int(100*(tight-best)/best)
    # if (hs < 0):
    #     hs = 0

    # warmup (iteration by which the mean is within 2.5% of the best mean)
    tgt = 1.015*best
    wu = 0
    while mean[best_hf][wu] >= tgt:
        wu = wu + 1

    # standard deviation (as a percentage) among invocations at peak performance
    st = int(100*std[best_hf][wu]/mean[best_hf][wu])

    # parallel efficiency (speedup versus ideal speedup)
    xone = perf[vm][par_hf]
    one = perf[vm_one][par_hf]
    std_one, mean_one, mini_one  = aggregate(one)
    pa = int(100*mean_one[wu]/(par_threads*mean[par_hf][wu]))  # perfect scaling -> 100

    # kernel
    vm = 'open-jdk-21.server.G1.t-32'
    hf = 2.0
    w = 0 # wall clock
    u = 0 # user
    k = 0 # kernel
    for res in kernel[vm][hf]:
        w += res[0]
        u += res[1]
        k += res[2]
    kpct = int(100*k/(u+k))



    # compiler
    hf = 2.0
    comp = {}
    iterations = 5
    compilers = ['G1.c1', 'G1.c1.comp', 'G1.c2', 'G1.c2.comp']
    all = compilers + ['G1']
    for c in all:
        vm = 'open-jdk-21.server.'+c+'.t-32'
        comp[c] = []
        for i in range(iterations):
            comp[c].append(0)
            for res in perf[vm][hf]:
                comp[c][i] += res[i]
            comp[c][i] = comp[c][i]/len(perf[vm][hf])
    final = {}
    for c in compilers:
        final[c] = comp[c][iterations-1]
    # compiler sensitivity
    vals = list(final.values())
    vals.sort()
    cr = vals[len(vals)-1]/vals[0] # compiler ratio (worst/best)
    cspct = int(100*(cr-1))
    # c2 comp
    cr = comp['G1.c2.comp'][0]/comp['G1'][0]
    ccpct = int(100*(cr-1))

    # LLC (cache sensitivity)
    hf = 2.0
    llc = {}
    caches = ['resctrl-0001', 'resctrl-ffff']
    for c in caches:
        vm = 'open-jdk-21.server.G1.'+c+'.t-32'
        llc[c] = 0
        for res in perf[vm][hf]:
            llc[c] += res[(len(res)-1)]
        llc[c] = llc[c]/len(res)
    llcs = llc['resctrl-0001']/llc['resctrl-ffff']
    llcpct = int(100*(llcs-1))

    # memory speed sensitivity
    hf = 2.0
    mem = {}
    cfgs = ['slow-memory', 'resctrl-ffff']
    for c in cfgs:
        vm = 'open-jdk-21.server.G1.'+c+'.t-32'
        mem[c] = 0
        for res in perf[vm][hf]:
            mem[c] += res[(len(res)-1)]
        mem[c] = mem[c]/len(res)
    mems = mem['slow-memory']/mem['resctrl-ffff']
    mempct = int(100*(mems-1))

    # turbo boost sensitivity
    hf = 2.0
    tb = {}
    cfgs = ['turbo-boost', 'resctrl-ffff']
    for c in cfgs:
        vm = 'open-jdk-21.server.G1.'+c+'.t-32'
        tb[c] = 0
        for res in perf[vm][hf]:
            tb[c] += res[(len(res)-1)]
        tb[c] = tb[c]/len(res)
    tbs = tb['resctrl-ffff']/tb['turbo-boost']
    tbpct = int(100*(tbs-1))

    # interpreter
    hf = 2.0
    intp = {}
    cfgs = ['interpreter', 'resctrl-ffff']
    for c in cfgs:
        vm = 'open-jdk-21.server.G1.'+c+'.t-32'
        intp[c] = 0
        for res in perf[vm][hf]:
            intp[c] += res[(len(res)-1)]
        intp[c] = intp[c]/len(res)
    ins = intp['interpreter']/intp['resctrl-ffff']
    inpct = int(100*(ins-1))

    # arm
    hf = 2.0
    intp = {}
    cfgs = ['taskset-0', 'arm.taskset-0']
    if 'open-jdk-21.server.G1.arm.taskset-0' in perf:
        for c in cfgs:
            vm = 'open-jdk-21.server.G1.'+c
            intp[c] = 0
            for res in perf[vm][hf]:
                intp[c] += res[(len(res)-1)]
            intp[c] = intp[c]/len(res)
        ins = intp['arm.taskset-0']/intp['taskset-0']
        armpct = int(100*(ins-1))
    else:
        armpct = None

    # intel
    hf = 2.0
    intp = {}
    cfgs = ['taskset-0', 'intel.taskset-0']
    for c in cfgs:
        vm = 'open-jdk-21.server.G1.'+c
        intp[c] = 0
        for res in perf[vm][hf]:
            intp[c] += res[(len(res)-1)]
        intp[c] = intp[c]/len(res)
    ins = intp['intel.taskset-0']/intp['taskset-0']
    intelpct = int(100*(ins-1))

    return best, np, wu, st, pa, tight, kpct, cspct, ccpct, llcpct, mempct, tbpct, inpct, armpct, intelpct;

def objectsizehisto():
    if alloc is None:
        return None;
    histo = {}
    total = 0
    for s in alloc['objects-by-size']:
        total = total + alloc['objects-by-size'][s]
        histo[s] = total

    return histo, total;

def get_percentile(histo, total, percentile):
    global alloc

    target = total * percentile

    t = 0
    for s in alloc['objects-by-size']:
        t = t + alloc['objects-by-size'][s]
        if t >= target:
            return s;

    return None; # should not reach here

def get_gc_stats():
    summary = {}
    for hf in gc:
        gctime = 0
        last = 0
        end = 0
        hs = []
        total_hs = 0
        for val in gc[hf]:
            end = val[0]    # we'll use the start time of last GC as the end point
            hs.append(val[1])
            total_hs = total_hs + val[1]
            last = val[2]   # we'll subtract the last pause, since it is after the nominal end
            gctime = gctime + last
        avg_hs = int(total_hs / len(hs))
        hs = sorted(hs)
        med_hs = hs[int(len(hs)/2)]
        totms = int(end * 1000)
        gcms = int(gctime - last)
        summary[hf] = [len(hs), totms, gcms, avg_hs, med_hs]
    return summary

def nominal():
    ap, np, wu, st, pa, tight, kpct, cspct, ccpct, llcpct, mempct, tbpct, inpct, armpct, intelpct = get_perf_stats()


    if (not alloc is None):
        histo, total = objectsizehisto()

        nom['AOM'] = int(get_percentile(histo, total, 0.5))
        desc['AOM'] = 'nominal median object size (bytes)'

        nom['AOS'] = int(get_percentile(histo, total, 0.1))
        desc['AOS'] = 'nominal 10-percentile object size (bytes)'

        nom['AOL'] = int(get_percentile(histo, total, 0.9))
        desc['AOL'] = 'nominal 90-percentile object size (bytes)'

        nom['AOA'] = int(alloc['bytes-allocated']/alloc['objects-allocated'])
        desc['AOA'] = 'nominal average object size (bytes)'

        nom['ARA'] = int(alloc['bytes-allocated']/(1000*ap))
        desc['ARA'] = 'nominal allocation rate (bytes / usec) ('+str(alloc['bytes-allocated'])+'/'+str(1000*ap)+')'

        nom['GTO'] = int(alloc['bytes-allocated']/(1024*1024*(max(minheap['open-jdk-21.ee.s.cp.gc-G1.t-32.f-10.n-1']))))
        desc['GTO'] = 'nominal memory turnover (total alloc bytes / min heap bytes)'

    hs = int(100*(tight-ap)/ap)
    if (hs < 0):
        hs = 0
    nom['GSS'] = hs
    desc['GSS'] = 'nominal heap size sensitivity (slowdown with tight heap, as a percentage) ('+str(tight)+'/'+str(ap)+')'

    nom['GMD'] = statistics.median(minheap['open-jdk-21.ee.s.cp.gc-G1.t-32.f-10.n-5'])
    desc['GMD'] = 'nominal minimum heap size (MB) for default size configuration (with compressed pointers)'

    sz='small'
    hscfg='open-jdk-21.sz-'+sz+'.ee.s.cp.gc-G1.t-1.f-10.n-1'
    if hscfg in minheap:
        nom['GMS'] = statistics.median(minheap[hscfg])
        desc['GMS'] = 'nominal minimum heap size (MB) for '+sz+' size configuration (with compressed pointers)'

    sz='large'
    hscfg='open-jdk-21.sz-'+sz+'.ee.s.cp.gc-G1.t-32.f-10.n-1'
    if hscfg in minheap:
        nom['GML'] = statistics.median(minheap[hscfg])
        desc['GML'] = 'nominal minimum heap size (MB) for '+sz+' size configuration (with compressed pointers)'

    sz='vlarge'
    hscfg='open-jdk-21.sz-'+sz+'.ee.s.cp.gc-G1.t-32.f-10.n-1'
    if hscfg in minheap:
        nom['GMV'] = statistics.median(minheap[hscfg])
        desc['GMV'] = 'nominal minimum heap size (MB) for '+sz+' size configuration (with compressed pointers)'


    nom['GMU'] = statistics.median(minheap['open-jdk-21.ee.s.up.gc-G1.t-32.f-10.n-1'])
    desc['GMU'] = 'nominal minimum heap size (MB) for default size without compressed pointers'

    one = max(minheap['open-jdk-21.ee.s.cp.gc-G1.t-32.f-10.n-1'])
    ten = max(minheap['open-jdk-21.ee.s.cp.gc-G1.t-32.f-10.n-10'])
    if (not ten is None):
        leakage = int(100*((ten/one)-1))
        if (leakage < 0):
            leakage = 0
        nom['GLK'] = leakage
        desc['GLK'] = 'nominal percent 10th iteration memory leakage (10 iterations / 1 iterations) ('+str(ten)+'/'+str(one)+')'

    nom['PET'] = np
    desc['PET'] = 'nominal execution time (sec)'

    nom['PWU'] = wu
    desc['PWU'] = 'nominal iterations to warm up to within 1.5% of best'

    nom['PPE'] = pa
    desc['PPE'] = 'nominal parallel efficiency (speedup as percentage of ideal speedup for '+str(par_threads)+' threads)'

    nom['PSD'] = st
    desc['PSD'] = 'nominal standard deviation among invocations at peak performance (as percentage of performance)'

    nom['PKP'] = kpct
    desc['PKP'] = 'nominal percentage of time spent in kernel mode (as percentage of user plus kernel time)'

    nom['PIN'] = cspct
    desc['PIN'] = 'nominal percentage slowdown due to using the interpreter (sensitivty to interpreter)'

    nom['PCS'] = cspct
    desc['PCS'] = 'nominal percentage slowdown due to worst compiler configuration compared to best (sensitivty to compiler)'

    nom['PCC'] = ccpct
    desc['PCC'] = 'nominal percentage slowdown due to aggressive c2 compilation compared to baseline (compiler cost)'

    nom['PLS'] = llcpct
    desc['PLS'] = 'nominal percentage slowdown due to 1/16 reduction of LLC capacity (LLC sensitivity)'

    nom['PMS'] = mempct
    desc['PMS'] = 'nominal percentage slowdown due to slower memory (memory speed sensitivity)'

    nom['PFS'] = tbpct
    desc['PFS'] = 'nominal percentage speedup due to enabling frequency scaling (CPU frequency sensitivity)'

    if (not bytecode is None):
        nom['BUB'] = int(bytecode['executed-bytecodes-unique']/1000)
        desc['BUB'] = 'nominal thousands of unique bytecodes executed'

        nom['BEF'] = int(1000*bytecode['executed-bytecodes-p9999']/bytecode['executed-bytecodes'])
        desc['BEF'] = 'nominal execution focus / dominance of hot code'

        nom['BUF'] = int(bytecode['executed-calls-unique']/1000)
        desc['BUF'] = 'nominal thousands of unique function calls'

        nom['BPF'] = int(bytecode['opcodes']['putfield']/(1000*ap))
        desc['BPF'] = 'nominal putfield per usec'

        nom['BGF'] = int(bytecode['opcodes']['getfield']/(1000*ap))
        desc['BGF'] = 'nominal getfield per usec'

        nom['BAS'] = int(bytecode['opcodes']['aastore']/(1000*ap))
        desc['BAS'] = 'nominal aastore per usec'

        nom['BAL'] = int(bytecode['opcodes']['aaload']/(1000*ap))
        desc['BAL'] = 'nominal aaload per usec'

    gc_summary = get_gc_stats()

    nom['GCC'] = gc_summary[2.0][0]
    desc['GCC'] = 'nominal GC count at 2X heap size (G1)'

    nom['GCP'] = int(100*(gc_summary[2.0][2]/gc_summary[2.0][1]))
    desc['GCP'] = 'nominal percentage of time spent in GC pauses at 2X heap size (G1) ('+str(gc_summary[2.0][2])+'/'+str(gc_summary[2.0][1])+')'

    nom['GCA'] = int(100*(gc_summary[2.0][3]/ten))
    desc['GCA'] = 'nominal average post-GC heap size as percent of min heap, when run at 2X min heap with G1 ('+str(gc_summary[2.0][3])+'/'+str(ten)+')'

    nom['GCM'] = int(100*(gc_summary[2.0][4]/ten))
    desc['GCM'] = 'nominal median post-GC heap size as percent of min heap, when run at 2X min heap with G1 ('+str(gc_summary[2.0][4])+'/'+str(ten)+')'

    # uarch
    if not armpct is None:
        nom['UAA'] = int(armpct)
        desc['UAA'] = 'nominal percentage change (slowdown) when running on ARM Calvium ThunderX v AMD Zen4 on a single core (taskset 0)'

    nom['UAI'] = int(intelpct)
    desc['UAI'] = 'nominal percentage change (slowdown) when running on Intel Alderlake v AMD Zen4 on a single core (taskset 0)'

    cfg = 'open-jdk-21.server.G1.t-32'
    hf = 2.0
    ua = uarch[cfg][hf]

    nom['UIP'] = int(100 * ua['IPC'])
    desc['UIP'] = 'nominal 100 x instructions per cycle (IPC) ( 100 x '+str(int(ua['IPC']))+' )'

    nom['USF'] = int(100 * ua['FE_BOUND'])
    desc['USF'] = 'nominal 100 x front end bound ( 100 x '+str(ua['FE_BOUND'])+') )'

    nom['USB'] = int(100 * ua['BE_BOUND'])
    desc['USB'] = 'nominal 100 x back end bound ( 100 x '+str(ua['BE_BOUND'])+') )'

    nom['UDC'] = int(ua['L1MPKI'])
    desc['UDC'] = 'nominal data cache misses per K instructions ( '+str(ua['L1MPKI'])+' )'

    nom['UDT'] = int(1000 * ua['DTLBMPKI'])
    desc['UDT'] = 'nominal DTLB misses per M instructions ( 1000 x '+str(ua['DTLBMPKI'])+' )'

    nom['ULL'] = int(1000 * (ua['LLCMPKI']))
    desc['ULL'] = 'nominal LLC misses M instructions ( 1000 x '+str(ua['LLCMPKI'])+' )'

    nom['UBM'] = int(1000 * (ua['BE_BOUND_MEMORY']))
    desc['UBM'] = 'nominal backend bound (memory) ( 1000 x '+str(ua['BE_BOUND_MEMORY'])+' )'

    nom['UBC'] = int(1000 * (ua['BE_BOUND_CPU']))
    desc['UBC'] = 'nominal backend bound (CPU) ( 1000 x '+str(ua['BE_BOUND_CPU'])+' )'

    nom['USC'] = int(1000 * (ua['SMT_CONTENTION']))
    desc['USC'] = 'nominal SMT contention ( 1000 x '+str(ua['SMT_CONTENTION'])+' )'

    nom['UBS'] = int(1000 * (ua['BAD_SPECULATION']))
    desc['UBS'] = 'nominal bad speculation ( 1000 x '+str(ua['BAD_SPECULATION'])+' )'

    nom['UBP'] = int(1000000 * (ua['BAD_SPECULATION_PIPELINE_RESTARTS']))
    desc['UBP'] = 'nominal bad speculation: pipeline restarts ( 1000000 x '+str(ua['BAD_SPECULATION_PIPELINE_RESTARTS'])+' )'

    nom['UBM'] = int(1000 * (ua['BAD_SPECULATION_MISPREDICTS']))
    desc['UBM'] = 'nominal bad speculation: mispredicts ( 1000 x '+str(ua['BAD_SPECULATION_MISPREDICTS'])+' )'

    print("# [value, mean, benchmark rank, description]")
    for x in sorted(nom):
        print(x+": ["+str(nom[x])+", '"+desc[x]+"']")

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
        load_yml(bmpath)
        nominal()

    exit(0)

if __name__ == "__main__":
    main(sys.argv[1:])
