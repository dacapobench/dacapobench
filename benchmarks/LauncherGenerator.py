# Launcher JAR Generator
#
# A launcher JAR contains a manifest file `META-INFO/MANIFEST.MF` with the
# following entries:
#
# - Manifest-Version: 1.0
# - Main-Class: Harness
# - Class-Path: All JARs necessary for running the particular benchmark

import sys

def main() -> int:
    """Entry point"""
    print(sys.argv)
    return 0

if __name__ == '__main__':
    sys.exit(main())
