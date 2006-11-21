Measurement collector tool
--------------------------

This directory contains the necessary patches to build the measurement
collector that produced the benchmark statistics in the paper, tech report 
and web pages.

To build the collector,

- download and unpack JikesRVM 2.4.5 from jikesrvm.sourceforge.net.
        cd jikesrvm-0.4.5
        patch -p0 <01-dynamicscope.patch
        patch -p0 <02-measurement.patch
        patch -p0 <03-10850.patch

        # set RVM_ROOT, RVM_BUILD, RVM_HOST_CONFIG and PATH as documented
        # in the JikesRVM user manual

        jconfigure FastAdaptiveMeasurement
        (cd $RVM_BUILD; ./jbuild)

- To run a benchmark through the collector

	rvm -X:gc:stressFactor=<interval> \
            -X:gc:cohortSize=<cohort-size> \
            -X:gc:xmlStats=true \
            -jar dacapo-2006-10.jar -c MMTkCallback <bm>

If running a non-dacapo benchmark, you need to arrange for the MMTk Harness
to be called to start and stop statistic collection.  The command line option

        -X:gc:harnessWholeRun=true

will collect statistics for the whole program run.
