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
my $publish_html = "$root_dir/www/regression/sanity";

produce_sanity_head_redirect_html($id);

# produce the index page which links to each vm's sanity results
sub produce_sanity_head_redirect_html() {
  my ($id) = @_;
  my $name="head.html";
  my $output;
  open $output, (">$publish_html/$name");
  my $writer = XML::Writer->new(OUTPUT => $output);
  my $ref = "$id/index.html";

  # create header
  $writer->startTag('html');
  $writer->startTag('head');

  # note that the sanity pages are expected to live in ../../sanity/$id/...
  $writer->emptyTag('meta',
      'http-equiv' => "REFRESH",
      content => "0;url=$ref");

  $writer->endTag('head');
  $writer->startTag('body');

  $writer->characters("This page should automatically redirect you to the sanity tests for the head current at ");
  $writer->startTag('a',
                    href => $ref);
  $writer->characters($ref);
  $writer->endTag('a');

  
  $writer->endTag('body');
  $writer->endTag('html');

  close $output;
}

