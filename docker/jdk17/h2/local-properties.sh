#! bin/bash

rm local.properties
touch local.properties
echo "make=/usr/bin/make" >> local.properties
echo "build.failonerror=true" >> local.properties
echo "jdk8home=/usr/lib/jvm/java-8-openjdk" >> local.properties

cat local.properties
