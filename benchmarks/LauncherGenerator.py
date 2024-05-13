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
import os
from pathlib import Path

def generate_jar(name: str, main_class: str, dest_dir: Path, jars):
    jar_name = name + ".jar"
    output_jar_path = dest_dir / jar_name
    relative_jar_paths = [str(os.path.relpath(jar, dest_dir)) for jar in jars]

    print("Generating launcher JAR " + str(output_jar_path))
    print("main_class = " + main_class)
    print("dest_dir = " + str(dest_dir))
    print("jars = " + str(relative_jar_paths))

    with zipfile.ZipFile(output_jar_path, mode="w") as archive:
        with archive.open("META-INF/MANIFEST.MF", "w") as meta:
            meta.write(bytes("Manifest-Version: 1.0\n", "utf-8"))
            meta.write(bytes("Main-Class: " + main_class + "\n", "utf-8"))
            meta.write(bytes("Class-Path: " + ':'.join(relative_jar_paths) + "\n", "utf-8"))

def main() -> int:
    help = "Usage: python LauncherGenerator.py Harness dacapo-evaluation-git-2cb70cd1.jar dacapo-evaluation-git-2cb70cd1/standalone"

    argc = len(sys.argv) - 1
    if argc != 3:
        print("3 arguments expected, found = " + str(argc))
        print(help)
        sys.exit(1)

    main_class = sys.argv[1]
    harness_jar = sys.argv[2]
    dest_dir = sys.argv[3]

    dest_dir_path = Path(dest_dir)
    harness_jar_path = Path(harness_jar)
    jar_parent_dir = harness_jar_path.with_suffix('')

    md5dir = zipfile.Path(harness_jar, "META-INF/md5/")
    for md5 in md5dir.iterdir():
        benchmark_name = md5.name.split('.')[0]
        print(benchmark_name)
        with md5.open(mode="r") as lines:
            jars = [jar_parent_dir / line.split()[1] for line in lines]
            jars += [harness_jar_path]
            generate_jar(benchmark_name, main_class, dest_dir_path, jars)

    return 0

if __name__ == '__main__':
    sys.exit(main())
