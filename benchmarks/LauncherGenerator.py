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
            # meta.write(bytes("Add-Exports: java.base/jdk.internal.ref java.base/jdk.internal.misc java.base/jdk.internal.ref java.base/sun.nio.ch java.management.rmi/com.sun.jmx.remote.internal.rmi java.rmi/sun.rmi.registry java.rmi/sun.rmi.server java.sql/java.sql java.base/jdk.internal.math java.base/jdk.internal.module java.base/jdk.internal.util.jar jdk.management/com.sun.management.internal\n", "utf-8"))
            # meta.write(bytes("Add-Opens: java.base/java.lang java.base/java.lang.module java.base/java.net java.base/jdk.internal.loader java.base/jdk.internal.ref java.base/jdk.internal.reflect java.base/java.io java.base/sun.nio.ch java.base/java.util java.base/java.util.concurrent java.base/java.util.concurrent.atomic java.base/java.nio\n", "utf-8"))
            meta.write(bytes("Class-Path: " + ' '.join(relative_jar_paths) + "\n", "utf-8"))

def main() -> int:
    help = "Usage: python LauncherGenerator.py sunflow Harness dacapo-evaluation-git-2cb70cd1.jar dacapo-evaluation-git-2cb70cd1/standalone"

    argc = len(sys.argv) - 1
    if argc != 4:
        print("3 arguments expected, found = " + str(argc))
        print(help)
        sys.exit(1)

    benchmark = sys.argv[1]
    main_class = sys.argv[2]
    harness_jar = sys.argv[3]
    dest_dir = sys.argv[4]

    dest_dir_path = Path(dest_dir)
    harness_jar_path = Path(harness_jar)
    jar_parent_dir = harness_jar_path.with_suffix('')

    benchmark_md5_name = "META-INF/md5/" + benchmark + ".MD5"
    md5_file = zipfile.Path(harness_jar, benchmark_md5_name)
    with md5_file.open(mode="r") as lines:
        jars = [jar_parent_dir / line.split()[1] for line in lines if line.endswith('.jar')]
        jars += [harness_jar_path]
        generate_jar(benchmark, main_class, dest_dir_path, jars)

    return 0

if __name__ == '__main__':
    sys.exit(main())
