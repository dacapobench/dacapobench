#!/usr/bin/env python3
#
# Take the nominal stats of all benchmarks and for each statistic
# find the median result and the rank of a given benchmark among
# the set (so we can see, for example, what the median min heap is,
# and rank the benchmarks according to their min heap).
#
# Author: Steve Blackburn 2023
#
import yaml
import sys
import getopt
import os.path
from os import walk

stats = {}        # summary stats
keys = {}
median = {}
mean = {}
count = {}
vals = {}

# metrics where lower is 'more interesting', so rank these the other way
# lower_is_better = [ 'BEF', 'GCA', 'GCC', 'GCM', 'PSD', 'PWU' ]
lower_is_better = [ ] # for now we're going to scrap this idea

def usage(errno):
    print ('usage: ',sys.argv[0], '-p <path to benchmark root>')
    sys.exit(errno)

def load_yml(path):
    global stats
    global keys

    bms = next(walk(path), (None, None, []))[1]
    
    for bm in bms:
        yml = path + '/'+bm+'/stats-nominal.yml'
        if os.path.exists(yml):
            with open(yml, 'r') as y:
                stats[bm] = yaml.load(y, Loader=yaml.Loader)
                for k in stats[bm]:
                    try:
                        keys[k].add(bm)
                    except:
                        keys[k] = { bm }

def normalize():
    for k in keys:
        vals[k] = []
        total = 0
        for bm in keys[k]:
            v = stats[bm][k][0]
            vals[k].append(v)
            total = total + v
        vals[k].sort(reverse=True)
        mean[k] = total / len(keys[k])
        median[k] = vals[k][int(len(keys[k])/2)]

    for bm in sorted(stats.keys()):
        print(bm)
        for k in stats[bm].keys():
            max = vals[k][0]
            min = vals[k][len(keys[k])-1]
            # score = int( 10 * (stats[bm][k][0] - min) / (max - min))
            r = 1
            for v in vals[k]:
                if v == stats[bm][k][0]:
                    break
                r = r + 1
            if (k in lower_is_better):
                print (k, r, score)
                r = (len(vals[k])-1)-r  # invert the ranking
                # score = 10 - score
                print (k, r, score)
            rank = str(r)+'/'+str(len(keys[k]))
            score = 10 - int(10 * r / len(keys[k]))
            if r == 1:
                score = 10

            l = [score, stats[bm][k][0], rank, min, median[k], max, stats[bm][k][1]]
            stats[bm][k] = l


def save_yml(path):
    bms = next(walk(path), (None, None, []))[1]
    for bm in bms:
        yml = path + '/'+bm+'/stats-nominal.yml'
        with open(yml, 'w') as y:
            y.write("# key: [score/10, value, rank, min, median, max, description]\n")
            yaml.dump(stats[bm], y, default_flow_style=None, width=10000)

def main(argv):
    path = None

    try:
        opts, args = getopt.getopt(argv, "hp:")
    except getopt.GetoptError as e:
        print ('ERROR: Incorrect arguments'.format(e.errno, e.sterror))
        usage(2)
    for opt,arg in opts:
        if opt == '-h':
            usage(0)
        elif opt == '-p':
            path = arg
    
    if path is None or not os.path.exists(path):
        print ('ERROR: You must specify a valid path')
        usage(2)
    else:
        load_yml(path)
        normalize()
        save_yml(path)

    exit(0)

if __name__ == "__main__":
    main(sys.argv[1:])
