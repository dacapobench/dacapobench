#!/usr/bin/perl

use BaseConfig;
use VMConfig;
use JarConfig;
use RunConfig;

my $id, $hour_id, $target_finish;

update();
command_line(\$id, \$hour_id, \$target_finish);
regression($id, $hour_id, $target_finish);

#
# Refresh all scripts before starting
#
sub update() {
  print "update $vcs repository\n";
  if ($vcs eq "svn") {
    print "performing svn update\n";
    system("svn update");
  } elsif ($vcs eq "hg") {
    print "performing hg update\n";
    system("( cd ../dacapo-base && hg pull --force && hg branch --force default && hg update --clean ) && cp -f ../dacapo-base/regression/* .");
  }
}

#
# Run one full regression
#
sub regression() {
  my ($id, $hour_id, $target_finish) = @_;
  do_build($id);
  do_versions($id);
  do_sanity($id, $hour_id, $target_finish);
  do_perf($id, $hour_id, $target_finish);
  do_upload($id, $hour_id);
  do_mailout($id, $hour_id, $mail_recipients)
}

#
# Mail out status
#
sub do_mailout() {
  my($id, $hour_id, $recipients) = @_;
  
  my $sendmail = "mail";
  my $subject = "Subject: [dacapo-regress] $id\n";
  my $send_to = "To: ".$recipients."\n";
  my $content = "";
  open(SENDMAIL, "|$sendmail") or die "Cannot open $sendmail: $!";
  print SENDMAIL $subject;
  print SENDMAIL $send_to;
  print SENDMAIL "Content-type: text/plain\n\n";
  print SENDMAIL $content;
  close(SENDMAIL);
}

#
# Upload results
#
sub do_upload() {
  my ($id, $hour_id) = @_;
  my $log;
  init_log($id, "upload", \$log);
  
  do_system($log, "rsync -a $root_dir/$csv_path $upload_target:$root_dir");
  do_system($log, "rsync -a $root_dir/$log_path/$id $upload_target:$root_dir/$log_path");
  do_system($log, "rsync -a $root_dir/$jar_path/*.jar $upload_target:$root_dir/$jar_path");
  my $plot_job = "ssh $upload_target \"cd $root_dir/$bin_path && ./plot.pl $id $hour_id\"";
  do_system($log, $plot_job);
  my $sanity_job = "ssh $upload_target \"cd $root_dir/$bin_path && ./sanity.pl $id $hour_id\"";
  do_system($log, $sanity_job);
  my $sanity_head_redirect_job = "ssh $upload_target \"cd $root_dir/$bin_path && ./sanity-head-redirect.pl $id $hour_id\"";
  do_system($log, $sanity_head_redirect_job);
}

#
# Dump per-run version info
#
sub do_versions() {
  my ($id) = @_;
  my $log;
  init_log($id, "versions", \$log);

  my $vm;
  my $job;
  foreach $vm (@vms) { 
    my $java = $root_dir."/".$vm_exe{$vm};
    my $job = "$java -version";
    do_system($log, "echo \"$job\" > $root_dir/$log_path/$id/$vm-version.txt 2>&1");
    do_system($log, "$job >> $root_dir/$log_path/$id/$vm-version.txt 2>&1");
  }
  $job = "cat /proc/cpuinfo";
  do_system($log, "echo \"$job\" > $root_dir/$log_path/$id/cpu-version.txt");
  do_system($log, "$job >> $root_dir/$log_path/$id/cpu-version.txt");
  $job = "uname -a";
  do_system($log, "echo \"$job\" > $root_dir/$log_path/$id/os-version.txt");
  do_system($log, "$job >> $root_dir/$log_path/$id/os-version.txt");
}

##
##
## Sanity and performance runs
##
##


#
# Do a complete set of sanity (correctness) runs and extract results
#
sub do_sanity() {
  my ($id, $hour_id, $target_finish) = @_;
  do_runs($id, $target_finish, "sanity", \@sanity_vms, \@sanity_jars, \@sanity_sizes, $sanity_iterations, $sanity_invocations);
  extract_results($id, $hour_id, "sanity");
}

#
# Do a complete set of performance runs and extract results
#
sub do_perf() {
  my ($id, $hour_id, $target_finish) = @_;
  do_runs($id, $target_finish, "perf", \@perf_vms, \@perf_jars, \@perf_sizes, $perf_iterations, $perf_invocations);
  extract_results($id, $hour_id, "perf");
}

#
# Do a complete set of sanity or performance runs
#
sub do_runs() {
  my ($id, $target_finish, $name, $vms, $jars, $sizes, $iterations, $invocations) = @_;
  my $logstr = "run-$name";
  my $logdir = "$root_dir/$log_path/$id/$logstr";
  my $vm, $bm, $size;
  my $log;
  init_log($id, "$logstr", \$log);

  print_stamp($log, "start");

  do_system($log, "mkdir -p $logdir");
  foreach $vm (@$vms) {
    do_system($log, "mkdir -p $logdir/$vm");
  }

  for (my $i = 0; $i < $invocations; $i++) {
    print $log "$i";
    foreach $jar_base (@$jars) {
      my @bms;
      get_bms($name, $jar_base, \@bms);
      print $log "$jar_base\n";
      foreach $bm (@bms) {
	print $log "$bm(";
	foreach $size (@$sizes) {
	  print $log "$size ";
	  foreach $vm (@$vms) {
	    print $log ".";
	    run_bm($id, $log, $name, "$logdir/$vm", $vm, $jar_base, $bm, $size, $iterations, $i);
	  }
	}
	print $log " ";
      }
      print $log ")\n";
    }
    if (time > $target_finish) {
      print "$start_time $target_finish (".localtime($start_time)." ".localtime($target_finish).")\n";
      last;
    }
  }

  print_stamp($log, "end");
}

#
# Run a single iteration of a benchmark
#
sub run_bm() {
  my ($id, $log, $name, $logdir, $vm, $jar_base, $bm, $size, $iterations, $invocation) = @_;
  my $logname, $java, $job, $jar, $run_id;
  $run_id = $jar_base."_".$bm."_".$size;
  $logname ="$logdir/$id"."_"."$invocation"."_".$run_id.".log";
  $java = $root_dir."/".$vm_exe{$vm};
  $jar = "$root_dir/$jar_path/dacapo-$jar_base.jar";

  echo_stamp($logname, "start");
  do_system($log, "$java -version >> $logname 2>&1");
  
  my $timeout = get_timeout($vm, $name."_".$run_id, $iterations);
  do_system($log, "$timedrun $timeout $java -jar $jar -s $size -n $iterations $bm >> $logname 2>&1");

  # now kill anything residual
  do_system($log, "killall -9 java; killall -9 rvm");

  echo_stamp($logname, "end");
  
}

#
# Return a timeout (in seconds) for a particular job
#
sub get_timeout() {
  my ($vm, $run_id, $iterations) = @_;
  my $elapsed;
  if (($elapsed = get_last_good_elapsed($vm, $run_id)) == -1) {
    if (($elapsed = get_last_good_elapsed($vm_canonical, $run_id)) == -1) {
      return $iterations * $default_timeout;
    } else {
      return int ($elapsed * $timeout_fallback_margin);
    }
  } else {
    return int ($elapsed * $timeout_margin);
  }
}

#
# Return the elapsed time for the last successful run of some job on a given vm
#
sub get_last_good_elapsed() {
  my ($vm, $run_id) = @_;
  my $line;
  my $elapsed = -1;
  my $csv = "$root_dir/$csv_path/$vm/$run_id.csv";
  open(CSV, "tail -20 $csv | grep -v FAILED | tail -1|");
  chomp($line = <CSV>);
#  print "==>$csv $line";
  close(CSV);
  if ($line =~ /^\d\d\d\d-\d\d-\d\d-...\d\d-\d\d/) {
    my @values = split(/,/,$line);
    $elapsed = $values[2];
  }
  return $elapsed;
}

#
# Get the list of bms
#
sub get_bms() {
  my ($name, $jar_base, $bms) = @_;
  if ($jar_base eq "2006-10-MR2") {
    @$bms = @old_bm_list;
  } else {
    open(BMS, "java -jar $root_dir/$jar_path/dacapo-$jar_base.jar -l|");
    chomp($line = <BMS>);
    close BMS;
    my @tmp = split(/ /,$line);
    my $bm;
    foreach $bm (@tmp) {
      if ($name eq "sanity" || !dont_run_perf($bm)) { #
	push @$bms, $bm;
      }
    } 
  }
}

sub dont_run_perf() {
  my ($bm) = @_;

  my $b;
  foreach $b (@dont_run_perf_bm_list) {
    if ($b eq $bm) {
      return 1;
    }
  }
  return 0;
}

#
# extract all results for a complete sanity or performance run
#
sub extract_results() {
  my ($id, $hour_id, $type) = @_;
  my $log;
  init_log($id, "results-$type", \$log);
  print_stamp($log, "start");

  my $run = "$id/run-$type";
  my $basedir = "$root_dir/$log_path/$run";
  my @vms;
  ls_to_array($basedir, \@vms);

  foreach $vm (@vms) {
    my @logs;
    my $dir = "$basedir/$vm";
    ls_to_array($dir, \@logs);
    foreach $logfile (@logs) {
      if ($logfile =~ /$processed_log_suffix$/) { next; }
      my $pass;
      my @times;
      my $elapsed;
      process_log($id, $log, $dir, $run, $vm, $logfile, \$pass, \@times, \$elapsed);
      update_csv($id, $log, $hour_id, $type, $logfile, $pass, \@times, $elapsed);
      rename_log($id, $log, $dir, $logfile);
    }
  }
  print_stamp($log, "end");
}


#
# add one set of results to a csv file, creating the file if necessary
#
sub update_csv() {
  my ($id, $log, $hour_id, $type, $logfile, $pass, $times, $elapsed) = @_;
  my $csv_name = get_csv_from_log_name($logfile, $type);
  my $invocation = get_invocation_from_log_name($logfile);
  my $resultid = "$id"."_"."$invocation";
  my $csvdir = "$root_dir/$csv_path/$vm";
  my $csvfile = "$csvdir/$csv_name";

  # create file with header if it does not already exist
  if ($pass || $type eq "sanity") {
    do_system($log, "mkdir -p $csvdir");
    if (!(-e $csvfile)) {
      do_system($log, "echo $csv_header > $csvfile");
    }
  }

  # add the result to the csv file
  if ($pass) {
    my $result = "\"$resultid, $hour_id, $elapsed";
    foreach $t (@$times) {
      $result .= ", $t";
    }
    $result .= "\"";
    do_system($log, "echo $result >> $csvfile");
  } elsif ($type eq "sanity") {
    my $result = "\"$resultid, $hour_id, $elapsed, FAILED\"";
    do_system($log, "echo $result >> $csvfile");
  }
}

#
# rename a run log file so that it won't be processed twice
#
sub rename_log() {
  my ($id, $log, $logdir, $logfile) = @_;
  # rename the log file so that it won't be re-processed
  my $processed_log = $logfile;
  $processed_log =~ s/[.]log$/$processed_log_suffix/;
  do_system($log, "mv $logdir/$logfile $logdir/$processed_log");
}

#
# return a csv file name, given a run log name
#
sub get_csv_from_log_name() {
  my ($logfilename, $type) = @_;
  $logfilename =~ s/.log$//;
  my ($id, $inv, $jar, $bm, $sz) = split /_/, $logfilename;
  return $type."_".$jar."_".$bm."_".$sz.".csv";
}

#
# return the invocation number, given a run log name
#
sub get_invocation_from_log_name() {
  my ($logfilename) = @_;
  $logfilename =~ s/.log$//;
  my ($id, $inv, $jar, $bm, $sz) = split /_/, $logfilename;
  return $inv;
}

#
# perform an ls on a directory, saving results to an array
#
sub ls_to_array() {
  my ($dir, $entries) = @_;
  my $line;

  open(DIR, "ls $dir |");
  while (chomp($line = <DIR>)) {
    push @$entries, $line;
  }
  close DIR;
}

#
# Process a run log, gathering results and producing error logs
#
sub process_log() {
  my ($id, $log, $dir, $run, $vm, $logfile, $pass, $times, $elapsed) = @_;
  my $start, $end;

  open(LOG, "$dir/$logfile");
  $$pass = 0;
  my $fail = 0;
  while (<LOG>) {
    if (/==== DaCapo .* in .* msec ====/) {
      my $time;
      ($time) = /in (\d+) msec/;
      push @$times, $time;
      if (/PASSED/ && !$fail) {
	$$pass = 1;
      } elsif (/FAILED/) {
	$fail = 1;
	$$pass = 0;
      }
    } elsif (/^(Start|Finish)ed at .* \(\d+\)/) {
      my $time;
      ($time) = /\((\d+)\)/;
      if (/Start/) {
	$start = $time;
      } else {
	$end = $time;
      }
    }
  }
  close(LOG);
  
  if (!$$pass && $run =~ /sanity/) {
    my $targetdir = "$root_dir/$pub_path/$run/$vm";
    do_system($log, "mkdir -p $targetdir");
    my $target = "$targetdir/$logfile";
    do_system($log, "head -c$max_sanity_log_bytes $dir/$logfile > $target");
  }
  if ($$pass) {
    $$elapsed = $end - $start;
  }

}

##
##
## Build benchmarks & vms
##
##

sub do_build() {
  my ($id) = @_;
  my $buildlog;
  init_log($id, "build", \$buildlog);
  build_dacapo_head($id, $buildlog);
  build_all_scms($id, $buildlog);
}

#
# initialize the dacapo head: clean, checkout, build
#
sub build_dacapo_head() {
  my ($id, $log) = @_;
  my $buildlog = "$log_path/$id/build-dacapo-$vcs.log";
  print $log "dacapo-$vcs ($buildlog) ";
  my $path = "$root_dir/$dacapo_head_build_path";
  $buildlog = "$root_dir/$buildlog";
  echo_stamp($buildlog, "start");

  print $log "clean... ";
  do_system($log, "cd $path; $dacapo_clean >> $buildlog 2>&1");

  print $log "update... ";
  do_system($log, "cd $path; $dacapo_co >> $buildlog 2>&1");

  print $log "build... ";
  do_system($log, "cd $path; $dacapo_build >> $buildlog 2>&1");

  echo_stamp($buildlog, "end");

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

  print $log "$vm ";
  $buildlog = "$log_path/$id/build-$vm.log";
  print $log "($buildlog) ";
  $buildlog = "$root_dir/$buildlog";
  echo_stamp($buildlog, "start");

  clean_scm($id, $log, $scm_vm_root, $vm);
  update_scm($id, $log, $buildlog, $scm_vm_root, $vm);
  if ($vm_scm_ok{$vm} == 1) {
    build_scm($id, $log, $buildlog, $scm_vm_root, $vm);
  }

  echo_stamp($buildlog, "end");
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
  $job = "(mkdir -p $root && cd $root && ".$vm_scm_co{$vm}.") >> $buildlog 2>&1";
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


##
##
## Utility functions
##
##


#
# run a system command and optionally log it
#
sub do_system() {
  my ($log, $job) = @_;

  if ($verbose) { print $log "Executing: [$job]\n"; }
  system($job); 
}

#
# set up a log for a particular phase
#
sub init_log() {
  my ($id, $name, $buildlog) = @_;
  system("mkdir -p $root_dir/$log_path/$id");
  open($$buildlog, ">$root_dir/$log_path/$id/$name.log");
}

#
# print a start or end timestamp to a log
#
sub print_stamp() {
  my ($log, $state) = @_;
  print $log timestamp_str($state)."\n";
}

#
# echo a start or end timestamp to a file
# (start truncates, end concatonates)
#
sub echo_stamp() {
  my ($log, $state) = @_;
  my $stamp = "\"".timestamp_str($state)."\"";
  if ($state eq "start") {
    system("echo $stamp > $log");
  } else {
    system("echo $stamp >> $log");
  }
}

#
# Generate a string for start and end timestamp events
#
sub timestamp_str() {
  my ($state) = @_;
  my $time = `date`;
  chomp($time);
  $str = (($state eq "start") ? "Started at " : "Finished at ")."$time (".time.")";
  return $str;
}

#
# Grab command line arguments
#
sub command_line() {
  my ($id, $hour_id, $target_finish) = @_;
  
  ($$id, $$hour_id, $$target_finish) = @ARGV;
  if (!(($$id =~ /^\d\d\d\d-\d\d-\d\d-...-\d\d-\d\d$/) &&
	$$hour_id >= 0 &&
	$$target_finish > time)) {
    die "Usage: regression.pl <id string> <hour id> <target finish time>";
  }
}
