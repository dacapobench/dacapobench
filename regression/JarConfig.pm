#
#
#
package JarConfig;
use BaseConfig qw($ant $root_dir);

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
	   );

$jar_path ="jar";
$dacapo_head_build_path = $jar_path;
$dacapo_head_jar = "head";
$dacapo_svn_dir = "dacapo-svn";
$dacapo_clean = "rm -rf $dacapo_svn_dir $root_dir/$jar_path/dacapo-$dacapo_head_jar.jar";
$dacapo_co = "svn co https://dacapobench.svn.sourceforge.net/svnroot/dacapobench/benchmarks/trunk $dacapo_svn_dir";
$dacapo_build = "(cd $dacapo_svn_dir/benchmarks && $ant dist && mv dacapo-*.jar $root_dir/$jar_path/dacapo-$dacapo_head_jar.jar)";

@old_bm_list = ("antlr", "bloat", "chart", "eclipse", "fop", "hsqldb", "jython", "lusearch", "luindex", "pmd", "xalan");

