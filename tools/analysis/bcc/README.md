# Bytecode Analysis for DaCapo

This tool exists to support simple analysis of the DaCapo benchmarks.

It uses the [ASM](https://asm.ow2.io/) bytecode transformation library to annotate bytecodes in the benchmarks at run time, and prints basic statistics.

The tool is built into the dacapo jar as part of the standard DaCapo build process.

Usage:

```
DACAPO=<root name of dacapo release>
java -javaagent:$DACAPO/jar/bccagent.jar -jar $DACAPO.jar -callback org.dacapo.analysis.BCCCallback <benchmark>
```

The `-javaagent` flag will activate the bytecode annotator.   The `-callback` will ensure that the provided callback will be used, which will capture statistics for iterations and output them at the end.
