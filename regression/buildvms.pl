#!/usr/bin/perl

use BaseConfig;
use VMConfig;
use JarConfig;
use RunConfig;

my $buildlog, $sanitylog, $perflog;
my $start_time = time;
my $target_finish = $start_time + $refresh_period_sec;

while() {
  my $id = getid();

  # initialization (build vms, dacapo)
  # do_build($id);

  # sanity
  do_sanity($id);

  # perf
  do_perf($id);

  $target_finish += $refresh_period_sec;
}

#my @bms;
#get_bms("head", \@bms);
#foreach $bm (@bms) { print "$bm+"; } print "\n";

#get_bms("2006-10-MR2", \@bms);
#foreach $bm (@bms) { print "$bm+"; } print "\n";


sub do_build() {
  my ($id) = @_;
  init_log($id, "build", \$buildlog);
  build_all_scms($id, $buildlog);
  build_dacapo_head($id, $buildlog);
}

sub do_sanity() {
  my ($id, $target_finish) = @_;
  do_runs($id, $target_finish, "sanity", \@sanity_vms, \@sanity_jars, \@sanity_sizes, $sanity_iterations, $sanity_invocations);
}

sub do_perf() {
  my ($id, $target_finish) = @_;
  do_runs($id, "perf", \@perf_vms, \@perf_jars, \@perf_sizes, $perf_iterations, $perf_invocations);
}

sub do_runs() {
  my ($id, $target_finish, $name, $vms, $jars, $sizes, $iterations, $invocations) = @_;
  my $logstr = "run-$name";
  my $logdir = "$root_dir/$log_path/$id-$logstr";
  my $vm, $bm, $size;
  my $log;
  init_log($id, "$logstr", \$log);

  my $start = `date`;
  print $log "starting $name at $start\n";

  do_system($log, "mkdir -p $logdir");
  foreach $vm (@$vms) {
    do_system($log, "mkdir -p $logdir/$vm");
  }

  for (my $i = 0; ($i < $invocations) && (time() < $target_finish; $i++) {
    print $log "$i";
    foreach $jar_base (@$jars) {
      my @bms;
      get_bms($jar_base, \@bms);
      print $log "$jar_base\n";
      foreach $bm (@bms) {
	print $log "$bm(";
	foreach $size (@$sizes) {
	  print $log "$size ";
	  foreach $vm (@$vms) {
	    print $log ".";
	    run_bm($id, $log, "$logdir/$vm", $vm, $jar_base, $bm, $size, $iterations, $i);
	  }
	}
	print $log " ";
      }
      print $log ")\n";
    }
  }

  my $end = `date`;
  print $log "finished $name at $end\n";
}

sub run_bm() {
  my ($id, $log, $logdir, $vm, $jar_base, $bm, $size, $iterations, $invocation) = @_;
  my $logname, $java, $job, $jar;
  $logname ="$logdir/$id"."_".$jar_base."_".$bm."_"."$size"."_"."$invocation.log";
  $java = $root_dir."/".$vm_exe{$vm};
  $jar = "$root_dir/$jar_path/dacapo-$jar_base.jar";

  do_system($log, "echo `date` > $logname");
  do_system($log, "$java -version >> $logname 2>&1");
  
  my $timeout = $iterations * $base_timeout;
  do_system($log, "$timedrun $timeout $java -jar $jar -s $size -n $iterations $bm >> $logname 2>&1");

  do_system($log, "echo `date` >> $logname");
}

#
# Get the list of bms
#
sub get_bms {
  my ($jar_base, $bms) = @_;
  if ($jar_base eq "2006-10-MR2") {
    @$bms = @old_bm_list;
  } else {
    open(BMS, "java -jar $root_dir/$jar_path/dacapo-$jar_base.jar -l|");
    chomp($line = <BMS>);
    @$bms = split(/ /,$line);
    close BMS;
  }
}

# performance

#
# set up a log for a particular phase
#
sub init_log {
  my ($id, $name, $buildlog) = @_;
  system("mkdir -p $root_dir/$log_path");
  open($$buildlog, ">$root_dir/$log_path/$id-$name.log");
}

#
# initialize the dacapo head: clean, checkout, build
#
sub build_dacapo_head {
  my ($id, $log) = @_;
  my $start = `date`;
  my $buildlog = "$log_path/$id-build-dacapo-$vcs.log";
  print $log "dacapo-$vcs ($buildlog) ";
  $buildlog = "$root_dir/$buildlog";
  system("echo 'Start: $start' > $buildlog");
  my $path = "$root_dir/$dacapo_head_build_path";

  print $log "clean... ";
  do_system($log, "cd $path; $dacapo_clean > $buildlog 2>&1");

  print $log "update... ";
  do_system($log, "cd $path; $dacapo_co >> $buildlog 2>&1");

  print $log "build... ";
  do_system($log, "cd $path; {$dacapo_build} >> $buildlog 2>&1");

  my $end = `date`;
  print $log "$end\n";
  system("echo 'End: $end' >> $buildlog");
  system("gzip $buildlog");
}


##
##
## Manage SCM controlled VMs (clean, checkout, build)
##
##

#
# initialize all of the scm vms
#
sub build_all_scms {
  my ($id, $log) = @_;
  my $vm;
  foreach $vm (@vms_scm) {
    init_scm($id, $log, $vm);
  }
}

#
# initialize an scm vm: clean, checkout & build
#
sub init_scm {
  my ($id, $log, $vm) = @_;
  my $scm_vm_root = $root_dir."/".$vm_scm_path;
  my $start = `date`;

  print $log "$vm ";
  $buildlog = "$log_path/$id-build-$vm.log";
  print $log "($buildlog) ";
  $buildlog = "$root_dir/$buildlog";
  system("echo 'Start: $start' > $buildlog");
  clean_scm($id, $log, $scm_vm_root, $vm);
  update_scm($id, $log, $buildlog, $scm_vm_root, $vm);
  if ($vm_scm_ok{$vm} == 1) {
    build_scm($id, $log, $buildlog, $scm_vm_root, $vm);
  }
  my $end = `date`;
  print $log "$end\n";
  system("echo 'End: $end' >> $buildlog");
  system("gzip $buildlog");
}

#
# clean an scm vm
#
sub clean_scm {
  my ($id, $log, $root, $vm) = @_;
  print $log "clean... ";
  system("cd $root; $vm_scm_clean{$vm}");
}

#
# update (checkout) an scm vm
#
sub update_scm {
  my ($id, $log, $buildlog, $root, $vm) = @_;
  print $log "checkout... ";
  $job = "(mkdir -p $root && cd $root && ".$vm_scm_co{$vm}.") > $buildlog 2>&1";
  if ($verbose) { print $log "\nexecuting: [$job]\n"; }
  system($job);
  if (!`grep \"$vm_scm_co_str{$vm}\" $buildlog`) {
    print $log " check out failed!\n";
    $vm_scm_ok{$vm} = -1;
  }
}

#
# build an scm vm
#
sub build_scm {
  my ($id, $log, $buildlog, $root, $vm) = @_;
  print $log "build... ";
  $job = "(mkdir -p $root && cd $root/$vm && ".$vm_scm_build{$vm}.") >> $buildlog 2>&1";
  if ($verbose) { print $log "\nexecuting: [$job]\n"; }
  system($job);
  if (!`grep \"$vm_scm_build_str{$vm}\" $buildlog`) {
    print $log " check out failed!\n";
    $vm_scm_ok{$vm} = -1;
  }
}

#
# Create a string that uniquely identifies this run
#
sub getid() {
    ($sec,$min,$hour,$mday,$mon,$year,$wday,$yday,$isdst) = localtime(time);
    $year += 1900;
    $mon += 1;
    $day = (Sun,Mon,Tue,Wed,Thu,Fri,Sat)[$wday];
    $id = sprintf("%4d-%2.2d-%2.2d-%s-%2.2d-%2.2d", $year, $mon, $mday, $day, $hour, $min);
    return $id;
}

$hour_seconds = 60 * 60;

sub getFinish() {
  my ($hours) 
}

#
# run a system command and optionally log it
#
sub do_system() {
  my ($log, $job) = @_;

  if ($verbose) { print $log "executing: [$job]\n"; }
  system($job); 
}
