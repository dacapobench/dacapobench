# Docker for H2 benchmark

This docker image includes all dependencies to build the **h2** benchmark for Java 17.

## How to build

1. Open `dacapobench` in terminal (so `dacapobench/` is your *pwd*)
2. Build this image with 

```shell
sudo docker build --tag dacapo:h2-17 docker/jdk17/h2
```

3. Run the container mounting the current directory and entering bash

```shell
sudo docker run -it --rm -v $(pwd):/repo dacapo:h2-17 /bin/sh
```

4. Now you are inside the container
```
cd benchmarks

# This script will create the local.properties file
sh ../docker/jdk17/h2/local-properties.sh

# Start the build
ant -Dbuild.target-jar=dacapo.jar h2
```
