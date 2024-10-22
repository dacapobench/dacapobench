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

def parse(indent, kernel, perfctr):
    invoke_re = re.compile(r'-----\n')
    warm_re = re.compile('completed warmup')
    pass_re = re.compile('PASSED in')
    time_re = re.compile(r'in \d+ msec')
    done_re = re.compile('End Tabulate Statistics')
    lable_re = re.compile('^pauses\t')
    stat_re = re.compile(r'^\d+\t')
    passed = False
    error = False
    times = ""
    uk = ""
    pc = ""
    labels = ()
    for line in sys.stdin:
        time = None
        if invoke_re.match(line):
            if kernel:
                uk = "["
            elif perfctr:
                pc = ""
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
            elif perfctr:
                print((" "*indent)+"- "+pc, end='')
            else:
                print((" "*indent)+"- "+times)
        elif passed and lable_re.search(line):
            labels = line.strip().split("\t")
        elif passed and stat_re.search(line):
            values = line.strip().split("\t")
            uk = uk + " "+str(int(values[3])//1000000)+", "+str(int(values[6])//1000)+", "+str(int(values[9])//1000)+" ]"
            # perfmap = {}
            for i in range(len(values)):
                if not pc == "":
                    pc += " "*(indent+2)
                pc = pc + labels[i] + ": " + values[i] +  "\n"

def main(argv):
    indent = 0
    kernel = False
    perfctr = False

    try:
        opts, args = getopt.getopt(argv, "hkpi:")
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
        elif opt == '-p':
            perfctr = True

    parse(indent, kernel, perfctr)

    exit(0)

if __name__ == "__main__":
    main(sys.argv[1:])