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
import re
import sys
import io
from pathlib import Path

# The specification for manifest files restricts line length to 72 bytes.
#
# Extra content should be followed by lines (length <= 72) staring with a single
# space.
#
# See https://docs.oracle.com/javase/7/docs/technotes/guides/jar/jar.html#Manifest_Specification
def format_line(input):
    """The input should not contain new line"""
    result = ""
    line_size = 0
    index = 0
    input_size = len(input)

    # invariant: `result` never ends with a new line and line length <= 71
    while index < input_size:
        c = input[index]
        assert c != '\n', "input should not contain new line"
        # reserve one byte for new line
        if line_size == 71:
            # empty lines with just a space cannot happen as index < input_size
            result = result + '\n'
            result = result + ' '
            line_size = 1

        result = result + c
        line_size = line_size + 1
        index = index + 1

    # result cannot already end with new line
    result = result + '\n'
    return bytes(result, "utf-8")

def zipfile_3_8_compat(context_manager):
    # https://docs.python.org/3.8/library/zipfile.html#zipfile.Path.open
    if sys.version_info >= (3, 9):
        return context_manager
    else:
        return io.TextIOWrapper(context_manager)

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
            meta.write(format_line("Manifest-Version: 1.0"))
            meta.write(format_line("Main-Class: " + main_class))

            # cassandra and h2o need special handling for modules
            #
            # Note: unlike the note in build.xml, the following line is sufficient for both
            if name == 'cassandra' or name == 'h2o':
                meta.write(format_line("Add-Opens: java.base/java.lang java.base/java.lang.module java.base/java.net java.base/jdk.internal.loader java.base/jdk.internal.ref java.base/jdk.internal.reflect java.base/java.io java.base/sun.nio.ch java.base/java.util java.base/java.util.concurrent java.base/java.util.concurrent.atomic java.base/java.nio"))

            meta.write(format_line("Class-Path: " + ' '.join(relative_jar_paths)))

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

    with zipfile_3_8_compat(md5_file.open(mode="r")) as lines:
        jars = []
        for line in lines:
            # use regex to simplify handling with line endings
            if re.match(r".*\.jar$", line):
                jars += [jar_parent_dir / line.split()[1]]
        jars += [harness_jar_path]
        generate_jar(benchmark, main_class, dest_dir_path, jars)

    return 0

if __name__ == '__main__':
    sys.exit(main())
