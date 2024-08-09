#!/usr/bin/env python3
#
# Scrape minheap data from output of the minheap script, and update the stats-minheap.yml files accordingly.
#
#
# Author: Steve Blackburn 2024
#
import yaml
import sys
import getopt
import os
import glob
import re

verbose = False

def usage(errno):
    print ('usage: ',sys.argv[0], '-i <path to input minheap files> -o <path to dacapobench root> -n <dacapo version name>')
    sys.exit(errno)

def parse_dryrun_line(line, name):
    line.pop(0)
    key = line.pop(0)
    # check the suite name
    suite = "-".join(key.split("-")[:-1])
    if not suite == name:
        print("[ERROR]: when parsing "+file+", the suite \'"+suite+"' does not match the name specified on the command line: \'"+name+"\'")
        exit(0)
    # extract the benchmark
    bm = key.split("-")[-1]
    # discard heap size
    hs = line.pop(0)
    # clean up command line
    cmdline = (" ".join(line)).rstrip()
    # generalize heap size expression
    cmdline = re.sub("-Xms\d+M", "-Xms<?>", cmdline)
    cmdline = re.sub("-Xmx\d+M", "-Xmx<?>", cmdline)
    # strip concrete directory from java binary
    cmdline = re.sub(r'\S+[/]([^/]+/bin/java)', r'\1', cmdline)
    # strip concrete directory from jar name
    cmdline = re.sub(r'\S+[/]([^/]+\.jar)', r'\1', cmdline)

    return (bm, cmdline)

def load_dryrun(path, name):
    global dryrun
    global cfgs

    files = glob.glob(path + "/*.txt")
    dryrun = {}
    tmp = {}
    for file in files:
        bn = os.path.basename(file)
        itr = bn.split("-")[1]
        if not itr in tmp:
            tmp[itr] = []
        cfg = ""
        with open(file, 'r') as dr:
            for l in dr:
                line = l.split(" ")
                if not line[0] == "[INFO]" and not line[0] == "[WARNING]" and not line[0] == ".":
                    if line[0] == "\t":
                        (bm, cmdline) = parse_dryrun_line(line, name)
                        if not bm in dryrun:
                            dryrun[bm] = {}
                        dryrun[bm][cfg] = cmdline
                    else:
                        cfg = line[0]+".n-"+itr
                        if not cfg in tmp[itr]:
                            tmp[itr].append(cfg)
                        # print("==>"+cfg)
    # fiddle with ordering of configs so the three most important are at top
    cfgs = tmp["1"]
    if "10" in tmp:
        cfgs.insert(1, tmp["10"][0])
    if "5" in tmp:
        cfgs.insert(1, tmp["5"][0])

def load_yml(path):
    global minheap

    files = glob.glob(path + "/*.yml")
    minheap = {}
    for file in files:
        if os.path.exists(file):
            bn = os.path.basename(file)
            itr = bn.split("-")[2]
            inv = bn.split("-")[3].split(".")[0]
            with open(file, 'r') as y:
                mh = yaml.load(y, Loader=yaml.Loader)
                for cfg in mh:
                    for suite in mh[cfg]:
                        for bm in mh[cfg][suite]:
                            print(".", end="")
                            if not suite in minheap:
                                minheap[suite] = {}
                            if not bm in minheap[suite]:
                                minheap[suite][bm] = {}
                            cf = cfg+".n-"+itr
                            if not cf in minheap[suite][bm]:
                                minheap[suite][bm][cf] = []
                            v = mh[cfg][suite][bm]
                            minheap[suite][bm][cf].append(v)
    print()

def maxlen(suite):
    max = 0
    for bm in sorted(minheap[suite]):
        for cfg in sorted(minheap[suite][bm]):
            vals = str(sorted(minheap[suite][bm][cfg]))
            l = len(cfg)+len(vals)+len(": ")
            if l > max:
                max = l
    return max


header = """#
# Minimum heap sizes in which the benchmark will run to completion, using
# -Xms<x> -Xmx<x> for various x and various configuarations.  Discovered
# by bisection search.  Numbers below are in MB and reflect results of
# five trials.
#"""

def write_yml(path, suite):
    if not suite in minheap:
        print ('ERROR: The version name you specified (\''+suite+'\') does not match the suite name/s found in the minheap files: '+''.join(minheap.keys()))
        usage(2)
    maxstrlen = maxlen(suite)
    for bm in sorted(minheap[suite]):
        print (bm)

        file = path+"/bms/"+bm+"/stats-minheap.yml"
        with open(file, 'w') as out:
            print (header, file=out)
            for cfg in cfgs:
                vals = str(sorted(minheap[suite][bm][cfg]))

                # only output if there was at least one value reported
                d = re.compile('\d')
                if d.search(vals):
                    # strip the ".jdk??" from the cfg
                    c = re.sub(r'([^.]+\.)jdk[^.]+\.(.+)', r'\1\2', cfg)
                    base = c+": "+vals
                    # align the comments
                    pad = " "*(maxstrlen - len(base))
                    line = base+pad+"#"+dryrun[bm][cfg]
                    print(line, file=out)
                else:
                    print("Skipping "+cfg+"->"+vals)

def main(argv):
    pathin = None
    pathout = None
    name = None

    try:
        opts, args = getopt.getopt(argv, "hvi:o:n:")
    except getopt.GetoptError as e:
        print ('ERROR: Incorrect arguments'.format(e.errno, e.sterror))
        usage(2)
    for opt,arg in opts:
        if opt == '-h':
            usage(0)
        elif opt == '-v':
            verbose = True
        elif opt == '-i':
            pathin = arg
        elif opt == '-o':
            pathout = arg
        elif opt == '-n':
            name = arg

    if pathin is None or not os.path.exists(pathin):
        print ('ERROR: You must specify a valid path to the minheap files')
        usage(2)


    if pathout is None or not os.path.exists(pathout) or not os.path.isdir(pathout) or not "bms" in os.listdir(pathout):
        print ('ERROR: You must specify a valid path to the benchmarks directory of a dacapo build tree')
        usage(2)
        
    if name is None:
        print ('ERROR: You must specify the benchmark suite name')
        usage(2)

    load_dryrun(pathin, name)
#    print(dryrun)
    load_yml(pathin)
    write_yml(pathout, name)
    # else:
    #     files = os.listdir(pathout)
    #     print (files)
    #     if not "bms" in files:
    #         print ('ERROR: You must specify a valid path to the "benchmarks" directory of the dacapo build tree')
    #     usage(2)

    exit(0)

if __name__ == "__main__":
    main(sys.argv[1:])
