#!/usr/bin/env python3
"""
Visualizing the DaCapo latency CSV dumps using Perfetto UI

Usage: ./tools/analysis/latency_perfetto.py /path/scratch/dacapo-latency-usec-simple-<n>.csv

The output .json.gz file can be visualized on https://ui.perfetto.dev/
Each request is shown as a slice.
The number of finished request for each thread over time is shown as a counter.

positional arguments:
  input            Path to simple latency CSV

optional arguments:
  -h, --help       show this help message and exit
  --output OUTPUT  Output path (default: "dacapo-latency.json.gz")
"""
import json
import gzip
from collections import defaultdict
import argparse


def build_parser():
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--output", help="Output path", default="dacapo-latency.json.gz", type=str
    )
    parser.add_argument("input", help="Path to simple latency CSV")
    return parser


def main():
    parser = build_parser()
    args = parser.parse_args()
    latency_csv_path = args.input
    events = []
    with open(latency_csv_path) as fd:
        lines = fd.readlines()
        finished_requests = defaultdict(int)
        for line in lines:
            parts = line.split(",")
            start = int(parts[0])
            end = int(parts[1])
            tid = int(parts[2])

            if start == 0 and end == 0:
                continue
            # Request as slices
            events.append({"name": "Req", "ph": "B", "ts": start, "pid": 0, "tid": tid})
            events.append({"name": "Req", "ph": "E", "ts": end, "pid": 0, "tid": tid})
            # The number of finished requests
            finished_requests[tid] += 1
            events.append(
                {
                    "name": "t{}".format(tid),
                    "ph": "C",
                    "ts": end,
                    "pid": 0,
                    "args": {"requests": finished_requests[tid]},
                }
            )
    events.append(
        {"name": "Process Start", "ph": "i", "ts": 0, "pid": 0, "tid": 0, "s": "p"}
    )
    with gzip.open(args.output, "wt") as fd:
        json.dump(events, fd)


if __name__ == "__main__":
    main()
