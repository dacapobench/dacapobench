# Launcher JAR Generator
#
# A launcher JAR contains a manifest file `META-INFO/MANIFEST.MF` with the
# following entries:
#
# - Manifest-Version: 1.0
# - Main-Class: Harness
# - Class-Path: All JARs necessary for running the particular benchmark

import sys
import zipfile

def generate_jar(name: str, dest_dir: str, jars):
    print("Generating launcher JAR " + name + ".jar")
    print("dest_dir = " + dest_dir)
    print("jars = " + str(jars))

def main() -> int:
    help = "Usage: python LauncherGenerator.py dacapo-evaluation-git-2cb70cd1.jar dacapo-evaluation-git-2cb70cd1/standalone"

    argc = len(sys.argv) - 1
    if argc != 2:
        print("2 arguments expected, found = " + str(argc))
        print(help)
        sys.exit(1)

    harness_jar = sys.argv[1]
    dest_dir = sys.argv[2]

    md5dir = zipfile.Path(harness_jar, "META-INF/md5/")
    for md5 in md5dir.iterdir():
        benchmark_name = md5.name.split('.')[0]
        print(benchmark_name)
        with md5.open(mode="r") as lines:
            jars = [line.split()[1] for line in lines]
            generate_jar(benchmark_name, dest_dir, jars)

    return 0

if __name__ == '__main__':
    sys.exit(main())
