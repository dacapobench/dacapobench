#
#
#
package VMConfig;
use BaseConfig qw($ant);

require Exporter;
@ISA = qw(Exporter);
@EXPORT = qw($vm_rel_path
	     $vm_scm_path
	     $vm_canonical
	     @vms
	     @vms_rel
	     @vms_scm
	     @perf_vms
	     @sanity_vms
	     %vm_str
	     %vm_exe
	     %vm_color
	     %vm_scm_clean
	     %vm_scm_co
	     %vm_scm_co_str
	     %vm_scm_build
	     %vm_scm_ok);

# paths to vms
$vm_rel_path = "vms/rel";
$vm_scm_path = "vms/scm";

# runtime options
$vm_run_args = "-Xms128M -Xmx1024M";

# VMs to be tested against a release
@vms_rel = ("ibm-java-i386-60",
	    "jdk1.5.0_12",
	    "jdk1.6.0_14",
	    "jrmc-3.1.0-1.6.0",
#	    "jikesrvm-2.9.3"
	    "jikesrvm-3.1.0");

# VM to be used for fallback (should be most robust well performing release VM)
$vm_canonical = "ibm-java-i386-60"; 

# VMs to be tested via scm checkout
@vms_scm = (
	    "jikesrvm-svn",
#	    "drlvm-svn",
#	    "cacao-hg"
	   );

# set of all VMs
@vms = (@vms_rel, @vms_scm);
@perf_vms = (@vms);
@sanity_vms = (@vms);

# descriptive string (for graphs etc)
%vm_str = ("ibm-java-i386-60" => "IBM 1.6",
	   "jdk1.5.0_12" => "Sun 1.5",
	   "jdk1.6.0_06" => "Sun 1.6",
	   "jdk1.6.0_14" => "Sun 1.6",
	   "jrmc-3.0.3-1.6.0" => "JRockit 1.6",
	   "jrmc-3.1.0-1.6.0" => "JRockit 1.6",
	   "jikesrvm-2.9.3" => "JikesRVM 2.9.3",
	   "jikesrvm-3.0.0" => "JikesRVM 3.0.0",
	   "jikesrvm-3.1.0" => "JikesRVM 3.1.0",
	   "jikesrvm-svn" => "JikesRVM svn",
	   "drlvm-svn" => "DRLVM svn",
#	   "cacao-hg" => "Cacao hg"
	  );


# see http://www.w3.org/TR/SVG/types.html#ColorKeywords
%vm_color =  ("ibm-java-i386-60" => "darkblue",
	   "jdk1.5.0_12" => "hotpink",
	   "jdk1.6.0_06" => "red",
	   "jdk1.6.0_14" => "red",
	   "jrmc-3.0.3-1.6.0" => "darkgreen",
	   "jrmc-3.1.0-1.6.0" => "darkgreen",
	   "jikesrvm-2.9.3" => "darkmagenta",
	   "jikesrvm-3.1.0" => "orchid",
	   "jikesrvm-svn" => "darkviolet",
	   "drlvm-svn" => "darkorange",
#	   "cacao-hg" => "Cacao hg"
	  );

my $jikesrvm_flags = "-X:processors=all ";
my $sun_flags = "-server ";
my $drlvm_flags = "-Xem:server ";

# full path for java executible (or equivalent)
%vm_exe = ("ibm-java-i386-60" => "$vm_rel_path/ibm-java-i386-60/bin/java $vm_run_args",
	   "jdk1.5.0_12" => "$vm_rel_path/jdk1.5.0_12/bin/java $sun_flags $vm_run_args",
	   "jdk1.6.0_06" => "$vm_rel_path/jdk1.6.0_06/bin/java $sun_flags $vm_run_args",
	   "jdk1.6.0_14" => "$vm_rel_path/jdk1.6.0_14/bin/java $sun_flags $vm_run_args",
	   "jrmc-3.0.3-1.6.0" => "$vm_rel_path/jrmc-3.0.3-1.6.0/bin/java $vm_run_args",
	   "jrmc-3.1.0-1.6.0" => "$vm_rel_path/jrmc-3.1.0-1.6.0/bin/java $vm_run_args",
	   "jikesrvm-2.9.3" => "$vm_rel_path/jikesrvm-2.9.3/dist/production_ia32-linux/rvm  $jikesrvm_flags $vm_run_args",
	   "jikesrvm-3.0.0" => "$vm_rel_path/jikesrvm-3.0.0/dist/production_ia32-linux/rvm  $jikesrvm_flags $vm_run_args",
	   "jikesrvm-3.1.0" => "$vm_rel_path/jikesrvm-3.1.0/dist/production_ia32-linux/rvm  $jikesrvm_flags $vm_run_args",
	   "jikesrvm-svn" => "$vm_scm_path/jikesrvm-svn/dist/production_ia32-linux/rvm  $jikesrvm_flags $vm_run_args",
	   "drlvm-svn" => "$vm_scm_path/drlvm-svn/working_vm/deploy/jdk/jre/bin/java $drlvm_flags $vm_run_args",
#	   "cacao-hg" => "$vm_scm_path/cacao/"
	  );
my $harmonysvn = "http://dacapo.anu.edu.au/svnroot/harmony/";
# command used to check out scm head
%vm_scm_co = ("jikesrvm-svn" => "svn co http://dacapo.anu.edu.au/svnroot/jikesrvm/rvmroot/trunk jikesrvm-svn",
#	      "drlvm-svn" => "svn co http://svn.apache.org/repos/asf/harmony/enhanced/trunk drlvm-svn",
	      "drlvm-svn" => "svn co ".$harmonysvn."harmony/enhanced/trunk drlvm-svn",
#	  "cacao-hg" => "hg clone http://mips.complang.tuwien.ac.at/hg/cacao cacao"
	 );

%vm_scm_co_str = ("jikesrvm-svn" => "Checked out revision",
		  "drlvm-svn" => "Checked out revision"
		 );

%vm_scm_build = ("jikesrvm-svn" => "/usr/bin/yes | ./bin/buildit -p localhost production",
		 "drlvm-svn" => "$ant -Dsvn.root=".$harmonysvn." -Dauto.fetch=true -Dhy.javac.compiler=modern all");

%vm_scm_build_str = ("jikesrvm-svn" => "Config  : production \[SUCCESS",
		     "drlvm-svn" => "BUILD SUCCESSFUL"
		    );

%vm_scm_clean = ("jikesrvm-svn" => "rm -rf jikesrvm-svn",
		 "drlvm-svn" => "rm -rf drlvm-svn");

%vm_scm_ok = ("jikesrvm-svn" => 1,
	      "drlvm-svn" => 1);
