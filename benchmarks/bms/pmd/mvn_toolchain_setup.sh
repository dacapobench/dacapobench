#!/bin/bash
# This script sets up the $HOME/.m2/toolchains.xml if it does not already exist

extract_java_version() {
    java -version 2>&1 | grep "version" | sed -E 's/.* version "[0-9]\.([0-9]+).*"/\1/g'
}

if [ ! -e $1 ]; then
    jver=$(extract_java_version)
    if [ $jver -eq 8 ]; then
        cat > $1 <<- EOM
<?xml version="1.0" encoding="UTF8"?>
<toolchains>
  <toolchain>
    <type>jdk</type>
    <provides>
      <version>1.7</version>
    </provides>
    <configuration>
      <jdkHome>$2</jdkHome>
    </configuration>
  </toolchain>
  <toolchain>
    <type>jdk</type>
    <provides>
      <version>1.8</version>
    </provides>
    <configuration> 
      <jdkHome>$JAVA_HOME</jdkHome>
    </configuration>
  </toolchain>
</toolchains>
EOM
  else
    echo "Building PMD benchmark needs Java 8!" >> /dev/stderr
    exit 1
  fi
fi