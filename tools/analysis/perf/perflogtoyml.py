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

def parse(indent, kernel):
    invoke_re = re.compile(r'-----\n')
    # completed warmup 8 in 1151 msec
    warm_re = re.compile('completed warmup')
    pass_re = re.compile('PASSED in')
    time_re = re.compile(r'in \d+ msec')
    done_re = re.compile('End Tabulate Statistics')
    stat_re = re.compile(r'^\d+\t')
    passed = False
    error = False
    yml = ""
    times = ""
    uk = ""
    for line in sys.stdin:
        time = None
        if invoke_re.match(line):
            if kernel:
                uk = "["
            else:
                times = "["
            error = False
        elif warm_re.search(line):
            time = time_re.search(line).group().split()[1]
            times = times + " "+time+","
        elif pass_re.search(line):
            passed = True
            time = time_re.search(line).group().split()[1]
            times = times + " "+time+" ]"
      #  elif passed: # and stat_re.search(line):
            # 82      0               560026361
         #   stuff = stat_re.search(line).group().split()
         #   uk = uk + " "+line+"]"
        elif not error and passed and done_re.search(line):
            passed = False
            if kernel:
                print((" "*indent)+"- "+uk)
            else:
                print((" "*indent)+"- "+times)
        elif passed and stat_re.search(line):
            stuff = line.split("\t")
            uk = uk + " "+str(int(stuff[3])//1000000)+", "+str(int(stuff[6])//1000)+", "+str(int(stuff[9])//1000)+" ]"

def main(argv):
    indent = 0
    kernel = False

    try:
        opts, args = getopt.getopt(argv, "hki:")
    except getopt.GetoptError as e:
        print ('ERROR: Incorrect arguments'.format(e.errno, e.sterror))
        usage(2)
    for opt,arg in opts:
        if opt == '-h':
            usage(0)
        if opt == '-k':
            kernel = True
        elif opt == '-i':
            indent = int(arg)

    parse(indent, kernel)

    exit(0)

if __name__ == "__main__":
    main(sys.argv[1:])