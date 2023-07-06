#!/usr/bin/env python3
#
# Read a log file from stdin and output perf results as yaml.
#
# Author: Steve Blackburn 2023
#
import sys
import getopt
import re

def usage(errno):

    print ('usage: ',sys.argv[0], '-i indent')
    sys.exit(errno)

def parse(indent):
    invoke_re = re.compile(r'-----\n')
    # completed warmup 8 in 1151 msec
    warm_re = re.compile('completed warmup')
    done_re = re.compile('PASSED in')
    time_re = re.compile(r'in \d+ msec')
    error = False
    yml = ""
    for line in sys.stdin:
        time = None
        if invoke_re.match(line):
            yml = "["
            error = False
        elif warm_re.search(line):
            time = time_re.search(line).group().split()[1]
            yml = yml + " "+time+","
        elif done_re.search(line):
            time = time_re.search(line).group().split()[1]
            yml = yml + " "+time+" ]"
            if not error:
                print((" "*indent)+"- "+yml)

def main(argv):
    indent = 0

    try:
        opts, args = getopt.getopt(argv, "hi:")
    except getopt.GetoptError as e:
        print ('ERROR: Incorrect arguments'.format(e.errno, e.sterror))
        usage(2)
    for opt,arg in opts:
        if opt == '-h':
            usage(0)
        elif opt == '-i':
            indent = int(arg)

    parse(indent)

    exit(0)

if __name__ == "__main__":
    main(sys.argv[1:])