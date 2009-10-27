#!/usr/bin/perl

use BaseConfig;
use VMConfig;
use JarConfig;
use RunConfig;
use strict;
use XML::Writer;
use Image::Magick;

# strictly we only use the $id
my ($id, $max_hour_id) = @ARGV;

my $root_dir = "/home/dacapo";
my $log_rel_dir = "regression/log";
my $log_dir = "$root_dir/$log_rel_dir";
my $publish_html = "$root_dir/www/regression/sanity/$id";

system("mkdir -p $publish_html/log");
make_sanity_set($id);

# make the complete set of sanity html pages and logs copies for
# a given id.
sub make_sanity_set() {
  my ($id) = @_;
  my @vms;
  my $vm;

  # obtain the set of vms from the run-sanity directory,
  # each VM has its own directory of the same name under which its logs are kept
  ls_to_array("$log_dir/$id/run-sanity",\@vms);

  # process each vm
  foreach $vm (@vms) {
    # create a space for the fail logs to be copied to so that they are 
    # visible through the web server
    system("mkdir -p $publish_html/log/$vm");
 
    # create the sanity results page for a vm
    produce_sanity_vm_html($id,$vm);
  }
  # create the index page which links to the results of each vm
  produce_sanity_index_html($id,@vms);
}

# produce the index page which links to each vm's sanity results
sub produce_sanity_index_html() {
  my ($id,@vms) = @_;
  my $name="index.html";
  my $output;
  my $vm;
  open $output, (">$publish_html/$name");
  my $writer = XML::Writer->new(OUTPUT => $output);

  # create header
  $writer->startTag('html');
  $writer->startTag('head');

  # note that the sanity pages are expected to live in ../../sanity/$id/...
  $writer->emptyTag('link',
                    rel => "stylesheet",
                    type =>"text/css",
                    href => "../../perf/dacapo.css");

  $writer->startTag('title');
  $writer->characters("Sanity Results for dacapo.jar head, $id");
  $writer->endTag('title');
  $writer->endTag('head');
  $writer->startTag('body');

  $writer->characters("The table below is a list of links to the sanity results for the DaCapo head (of $id) were run for each VM tested.");

  # write a table of VM sanity page links
  $writer->startTag('table', border => 0);

  # write links for each VM (sort the VMs alphabetically)
  foreach $vm (sort(@vms)) {
    $writer->startTag('tr');
    write_cell($writer, 'td', "$vm","$vm.html");
    $writer->endTag('tr');
  }
  # write table end  

  $writer->endTag('table');
  
  $writer->endTag('body');
  $writer->endTag('html');

  close $output;
}

#
sub produce_sanity_vm_html() {
  my ($id,$vm) = @_;
  my @results;
  my $result;

  ls_to_array("$log_dir/$id/run-sanity/$vm",\@results);

  my @bms;
  my @sizes;

  my $result;
  my @sizes;
  my @bms;
  my %bm_results;
  my %bm_logfile;
  my $bm;
  my $size;

  foreach $result (@results) {
    $_ = $result;
    if (/[~_]*_0_head_([a-zA-Z][a-zA-Z0-9]*)_([a-zA-Z]*)\.plog/) {
      ($bm,$size)=($1,$2);
      $bm_results{$bm}{$size} = find_status($id,$vm,$result);
      $bm_logfile{$bm}{$size} = $result;
      add_unique($size,\@sizes);
    }
  }
  produce_benchmarks_html($id,$vm,\@sizes,\%bm_results,\%bm_logfile);
}

sub produce_benchmarks_html() {
  my ($id,$vm,$sizes,$bm_results,$bm_logfile) = @_;
  my $name="$vm.html";
  my $output;
  my $size;
  my $bm;
  open $output, (">$publish_html/$name");
  my $writer = XML::Writer->new(OUTPUT => $output);
  $writer->startTag('html');
  $writer->startTag('head');
  $writer->emptyTag('link',
                    rel => "stylesheet",
                    type =>"text/css",
                    href => "../../perf/dacapo.css");
  $writer->startTag('title');
  $writer->characters("Sanity Results $vm_str{$vm} for dacapo.jar head, $id");
  $writer->endTag('title');
  $writer->endTag('head');
  $writer->startTag('body');

  $writer->characters("The table below displays the results of each benchmark in DaCapo head (of $id) for all available sizes running on the $vm_str{$vm} VM.  Each has one of three possible states: passed if the sanity run was successful; missing if no log was produced; and failed which is a link to the log for that particular run.");

  # write_table(\$writer,$id,$vm,$sizes,$bm_results);
  my @bms = sort(keys %$bm_results);

  # write table start
  $writer->startTag('table', border => 1);
  # write table headers
  $writer->startTag('tr');

  # top left cell is empty
  $writer->emptyTag('th');
  foreach $size (sort(@$sizes)) {
    write_cell($writer, 'th', $size,);
  }
  $writer->endTag('tr');

  # write each benchmark's results
  foreach $bm (@bms) {
    $writer->startTag('tr');
    write_cell($writer, 'th', $bm,);
    # write the results for each size for this benchmark
    foreach $size (sort(@$sizes)) {
      if (! $$bm_results{$bm}) {
        write_cell($writer, 'td', "MISSING",);
      } 
      elsif ($$bm_results{$bm}{$size}) {
        write_cell($writer, 'td', "passed",);
      }
      else {
        # we only copy the logs for each sanity result that failed
        system("cp $log_dir/$id/run-sanity/$vm/$$bm_logfile{$bm}{$size} $publish_html/log/$vm/$$bm_logfile{$bm}{$size}");
        write_cell($writer, 'td', "failed","log/$vm/$$bm_logfile{$bm}{$size}");
      }
    }
    $writer->endTag('tr');
  }
  # write table end  

  $writer->endTag('table');
  
  $writer->endTag('body');
  $writer->endTag('html');

  close $output;
}

# write a table data element (cell), in this case a cell either contains
# text or a link and associated text label.
sub write_cell() {
  my ($writer,$tag,$text,$ref) = @_;
  
  $writer->startTag($tag);
  if ($ref) {
    $writer->startTag('a',
                      href => $ref);
    $writer->characters($text);
    $writer->endTag('a');
  }
  else {
    $writer->characters($text);
  }
  $writer->endTag($tag);
}

# unique add to a list
sub add_unique() {
  my ($v,$listref) = @_;
  if (! contains($v,$listref)) {
    push @$listref, $v;
  }
}

# check if a list contains an element
sub contains() {
  my ($v,$listref) = @_;
  my $lv;

  foreach $lv (@$listref) {
    if ($v eq $lv) {
      return 1;
    }
  }
  return 0;
}

# get the status of a log file, if one or more FAILED messages 
# are in the log then the result is assumed to be a fail.
sub find_status() {
  my ($id,$vm,$result) = @_;

  open RESULTFILE, "<$log_dir/$id/run-sanity/$vm/$result" or return 0;

  while (<RESULTFILE>) {
    if (/PASSED/) {
      close RESULTFILE;
      return 1;
    }
  } 

  close RESULTFILE;
  return 0;
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

