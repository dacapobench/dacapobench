#!/bin/bash
# This script sets up the $HOME/.m2/toolchains.xml if it does not already exist

extract_java_version() {
    java -version |& grep "java version" | sed '{ s/java version "[0-9]\.\([0-9]\+\).*"/\1/ }'
}

if [ ! -e $1 ]; then
    jver=`extract_java_version`
    if [ $jver -ge 8 ]; then
        cat > $1 <<- EOM
<?xml version="1.0" encoding="UTF8"?>
<toolchains>
  <toolchain>
    <type>jdk</type>
    <provides>
      <version>1.7</version>    <!-- # though the actual version might be greater, pmd requires 1.7 here. -->
    </provides>
    <configuration> 
      <jdkHome>$JAVA_HOME</jdkHome>
    </configuration>
  </toolchain>
  <toolchain>
    <type>jdk</type>
    <provides>
      <version>1.$jver</version>
    </provides>
    <configuration> 
      <jdkHome>$JAVA_HOME</jdkHome>
    </configuration>
  </toolchain>
</toolchains>
EOM
  else
    echo "PMD benchmark needs Java >= 1.8!" >> /dev/stderr
    exit 1
  fi
fi