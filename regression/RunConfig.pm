package RunConfig;
use JarConfig qw($dacapo_head_jar);

require Exporter;
@ISA = qw(Exporter);
@EXPORT = qw($timedrun
	     $default_timeout
	     $timeout_margin
	     $timeout_fallback_margin
	     @perf_jars
	     @sanity_jars
	     @perf_sizes
	     @sanity_sizes
	     $perf_iterations
	     $sanity_iterations
	     $perf_invocations
	     $sanity_invocations
	   );

$default_timeout = 180;
$timeout_margin = 1.5;         # kill a job if it is > 50% slower than recent runs by same VM
$timeout_fallback_margin = 3;  # kill a job if it is > 3 X slower than recent runs by canonical VM
$timedrun = "./timedrun.pl -t ";


#@perf_jars = ("2006-10-MR2", $dacapo_head_jar);
@perf_jars = ("9.12-bach", $dacapo_head_jar);
@sanity_jars = ($dacapo_head_jar);
@perf_sizes = ("default");
@sanity_sizes = ("small", "default", "large");

$perf_iterations = 10;
$sanity_iterations = 1;

$perf_invocations = 8;
$sanity_invocations = 1;

