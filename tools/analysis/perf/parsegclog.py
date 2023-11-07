#!/usr/bin/env python3

import sys
import os
import gzip
import re

def main():
    parse_log(sys.argv[1])

def parse_log(file: os.PathLike):
    opener = open
    filename, ext = os.path.splitext(file)

    if ext == ".gz":  # gzipped version of the file, we need gzip.open
        opener = gzip.open

    gc = []
    with opener(file,'rt') as f:
        for line in f:


            # [7.210s][info][gc] GC(935) Pause Young (Prepare Mixed) (G1 Evacuation Pause) 19M->16M(30M) 1 129ms
            # Pause Young (Mixed) (G1 Evacuation Pause) 17M->13M(30M) 0.612ms
            # Pause Young (Normal) (G1 Evacuation Pause) 17M->13M(30M) 0.886ms
            # Pause Young (Concurrent Start) (G1 Evacuation Pause) 20M->17M(30M) 0.663ms
            # Pause Remark 19M->18M(30M) 6.612ms
            # Pause Cleanup 18M->18M(30M) 0.012ms
            # [3.870s][info][gc] GC(404) Pause Full (System.gc()) 13M->9M(30M) 15.774ms
            # [1.391s][info][gc] GC(92) Pause Full (G1 Evacuation Pause) 95M->89M(98M) 45.709ms


            parse = re.split("[\[\] ]", line.strip())
            if parse[8] == "Pause":
                start = float(parse[1][:-1])
                idx = 10
                if parse[9] == "Full":
                    if parse[10] == "(System.gc())":
                        idx = 11
                    else:
                        idx = 13
                elif parse[10] == "(Normal)" or parse[10] == "(Mixed)":
                    if (parse[14] == "(Evacuation"):
                        idx = 16
                    else:
                        idx = 14
                elif parse[9] == "Young":
                    if (parse[15] == "(Evacuation"):
                        idx = 17
                    else:
                        idx = 15
                
                # print(parse)
                # print(str(idx))
                # if idx == 14:
                #     print(parse[14])
                size = int(re.split("[>M]",parse[idx])[2])
                pause = float(parse[idx+1][:-2])
                gc.append([start, size, pause])
                # print(start, heap, pause)
                #print(start, heap[2], pause)
#            else:
#                print('got: ',parse)
#    print(file)
    print(gc)

if __name__ == "__main__":
    main()
