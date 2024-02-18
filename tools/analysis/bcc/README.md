# Bytecode Analysis for DaCapo

This tool exists to support simple analysis of the DaCapo benchmarks.

It uses the [ASM](https://asm.ow2.io/) bytecode transformation library to annotate bytecodes in the benchmarks at run time, and prints basic statistics.

The tool is built into the dacapo jar as part of the standard DaCapo build process.

Example usage:

```
DACAPO=<prefix of path to dacapo jar>
for bm in `java -jar $DACAPO.jar -l 2>/dev/null`; do
    java -Ddacapo.bcc.yml=bms/$bm/stats-bytecode.yml -Djava.security.manager=allow -javaagent:$DACAPO/jar/bccagent.jar -jar $DACAPO.jar -callback org.dacapo.analysis.BCCCallback $bm
done
```

* The `-javaagent` flag will activate the bytecode annotator.
* The `-callback` will ensure that the provided callback will be used, which will capture statistics for iterations and output them at the end.
* `-Ddacapo.bcc.yml` specifies the file where the yml will be written (if undefined, output will go to stdout).
* `-Djava.security.manager=allow` is required for the cassandra workload when using more recent JVMs.
