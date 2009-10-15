#
#
#
package JarConfig;
use BaseConfig qw($ant_opts $root_dir);

require Exporter;
@ISA = qw(Exporter);
@EXPORT = qw($jar_path
	     $dacapo_head_build_path
	     $dacapo_head_jar
	     $dacapo_svn_dir
	     $dacapo_clean
	     $dacapo_co
	     $dacapo_build
	     @old_bm_list
	     @retired_bm_list
	     @dont_run_perf_bm_list
	   );

$jar_path ="jar";
$dacapo_head_build_path = $jar_path;
$dacapo_head_jar = "head";
$dacapo_svn_dir = "dacapo-svn";
$dacapo_clean = "rm -rf $dacapo_svn_dir $root_dir/$jar_path/dacapo-$dacapo_head_jar.jar";
$dacapo_co = "svn co http://dacapo.anu.edu.au/svnroot/dacapobench/benchmarks/trunk $dacapo_svn_dir";
my $dacapo_java_home = "/usr/lib/jvm/java-1.5.0-sun";
my $dacapoant = "export ANT_OPTS=\"$ant_opts\" && export JAVA_HOME=$dacapo_java_home && ant";
$dacapo_build = "(cd $dacapo_svn_dir/benchmarks && $dacapoant dist && mv dacapo-*.jar $root_dir/$jar_path/dacapo-$dacapo_head_jar.jar)";

@old_bm_list = ("antlr", "bloat", "chart", "eclipse", "fop", "hsqldb", "jython", "lusearch", "luindex", "pmd", "xalan");
@retired_bm_list = ("antlr", "bloat", "chart", "hsqldb");
@dont_run_perf_bm_list = (); # ("tradebeans", "tradesoap");
