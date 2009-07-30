#!/usr/bin/perl

use BaseConfig;
use VMConfig;
use JarConfig;
use RunConfig;
use strict;

# strictly we only use the $id
my ($id, $max_hour_id) = @ARGV;

my $root_dir = "/home/dacapo";
my $log_rel_dir = "regression/log";
my $log_dir = "$root_dir/$log_rel_dir";
my $publish_html = "$root_dir/www/regression/sanity";

my $latest_link = "$publish_html/latest";
my $latest_dir = "$publish_html/$id";

system("rm -f $latest_link");
system("ln -s $latest_dir $latest_link");

