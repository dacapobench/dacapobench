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
	     $dacapo_vcs_dir
	     $dacapo_clean
	     $dacapo_co
	     $dacapo_build
	     @old_bm_list
	     @retired_bm_list
	     @dont_run_perf_bm_list
             $vcs
	   );

$jar_path ="jar";
$dacapo_head_build_path = $jar_path;
$dacapo_head_jar = "head";

$vcs = "hg";

$dacapo_svn_dir = "dacapo-svn";
$dacapo_hg_dir  = "dacapo-hg";

my %dacapo_vcs_dirs = ( "svn" => $dacapo_svn_dir, "hg" => $dacapo_hg_dir );
$dacapo_vcs_dir  = $dacapo_vcs_dirs{$vcs};
my $dacapo_java_home = "/usr/lib/jvm/java-1.5.0-sun";
my $dacapoant = "export ANT_OPTS=\"$ant_opts\" && export JAVA_HOME=$dacapo_java_home && ant";
my %vcs_config = (
   "svn" => { "clean"    => "rm -rf $dacapo_svn_dir $root_dir/$jar_path/dacapo-$dacapo_head_jar.jar",
              "co"       => "svn co http://dacapo.anu.edu.au/svnroot/dacapobench/benchmarks/trunk $dacapo_svn_dir",
              "build"    => "(cd $dacapo_svn_dir/benchmarks && $dacapoant dist && mv dacapo-*.jar $root_dir/$jar_path/dacapo-$dacapo_head_jar.jar)" },
   "hg"  => { "clean"    => "rm -rf $dacapo_hg_dir/* $root_dir/$jar_path/dacapo-$dacapo_head_jar.jar",
              "co"       => "( cd $dacapo_hg_dir && hg pull --force && hg branch --force default && hg update --clean )",
              "build"    => "(cd $dacapo_hg_dir/benchmarks && $dacapoant dist && mv dacapo-*.jar $root_dir/$jar_path/dacapo-$dacapo_head_jar.jar)" }
);

# $dacapo_clean = "rm -rf $dacapo_svn_dir $root_dir/$jar_path/dacapo-$dacapo_head_jar.jar";
# $dacapo_co = "svn co http://dacapo.anu.edu.au/svnroot/dacapobench/benchmarks/trunk $dacapo_svn_dir";
# $dacapo_build = "(cd $dacapo_svn_dir/benchmarks && $dacapoant dist && mv dacapo-*.jar $root_dir/$jar_path/dacapo-$dacapo_head_jar.jar)";
$dacapo_clean = $vcs_config{$vcs}{"clean"};
$dacapo_co    = $vcs_config{$vcs}{"co"};
$dacapo_build = $vcs_config{$vcs}{"build"};

@old_bm_list = ("antlr", "bloat", "chart", "eclipse", "fop", "hsqldb", "jython", "lusearch", "luindex", "pmd", "xalan");
@retired_bm_list = ("antlr", "bloat", "chart", "derby", "hsqldb");
@dont_run_perf_bm_list = ();
1;
