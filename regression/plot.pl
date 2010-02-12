#!/usr/bin/perl

use BaseConfig;
use VMConfig;
use JarConfig;
use RunConfig;
use strict;
use XML::Writer;
use Image::Magick;

my $image_height = 400;
my $image_width = 600;
my $font_family = "arial";
my $small_image_resize = "30%";

my $publish_png = "/home/dacapo/www/regression/perf/png";
my $publish_html = "/home/dacapo/www/regression/perf";

my $plot_r_margin = 0.015;
my $plot_l_margin = 0.15;
my $plot_t_margin = 0.09;
my $plot_b_margin = 0.15;
my $tick_height = 0.025;
my $font_height = 0.04;
my $line_stroke = 3;
my $normalize_to_iteration = 0;

my $plot_height = int ($image_height * (1-($plot_t_margin+$plot_b_margin)));
my $plot_y_offset = int ($image_height * $plot_t_margin);
my $plot_width = int($image_width * (1-($plot_l_margin+$plot_r_margin)));
my $plot_x_offset = int ($image_height * $plot_l_margin);

my ($id, $max_hour_id) = @ARGV;

my %bmlist;

get_targets(\%bmlist);
make_all_svg(\%bmlist, $max_hour_id);
make_all_png();
make_all_tables(\%bmlist);
system("cp ../log/$id/*version.txt $publish_html");
system("cp ../jar/*.jar $publish_html");


sub make_all_tables() {
  my ($bmlistref) = @_; 
  my $jar;
  foreach $jar (sort keys %$bmlistref) {
    my @bms = sort keys %{$$bmlistref{$jar}};
    produce_html($jar, \@bms);
  }
}

sub make_all_svg() {
  my ($bmlistref, $max_hour_id) = @_; 
  system("rm -f $root_dir/$svg_path/*.svg");

  produce_legends();

  my $jar;
  foreach $jar (sort keys %$bmlistref) {
    my $bm;
    foreach $bm (sort keys %{$$bmlistref{$jar}}) {
      produce_bm_name($bm, $jar);
      plot_graphs($bm, $jar, $max_hour_id);
    }
  }
}

sub produce_legends() {
  my $iter;
  for($iter = 1; $iter <= 10; $iter++) {
    produce_column_name("iteration_$iter"."_name.svg", "Iteration $iter");
  }
  produce_column_name("warmup_name.svg", "Warmup");

  my $vm;
  foreach $vm (@vms) {
    produce_vm_legend($vm);
  }
}

sub make_all_png() {
  my @svgs;
  ls_to_array("$root_dir/$svg_path", \@svgs);
  my $svg;
  my $job;
  foreach $svg (@svgs) {
    $_ = $svg;
    if (/_1.svg/ || /_3.svg/ || /_10.svg/ || /warmup.svg/ || /legend.svg/ | /_name.svg/) {
    $job = "java -Xms200M -Xmx500M -jar $root_dir/$bin_path/batik-1.7/batik-rasterizer.jar $root_dir/$svg_path/$svg -d $publish_png";
    system($job);
  }
  }
  my @pngs;
  ls_to_array("$publish_png", \@pngs);
  my $large;
  foreach $large (@pngs) {
    if (!($large =~ /_small.png$/)) {
      print ".";
      my $small = $large;
      $small =~ s/.png$/_small.png/;
      $job = "cp -f $publish_png/$large $publish_png/$small";
      system($job);
      $job = "mogrify -resize $small_image_resize $publish_png/$small";
      system($job);
    }
  }
}

sub get_targets() {
  my ($bmlistref) = @_;

  my $vm;
  my %jars;
  foreach $vm (@vms) {
    my @files;
    ls_to_array("$root_dir/$csv_path/$vm", \@files);
    my $f;
    foreach $f (@files) {
      if ($f =~ /^perf/) {
	my ($pre, $jar, $bm, $suf) = split(/_/, $f);
	if (!(($jar eq "head") && is_retired($bm))) {
	  ${$$bmlistref{$jar}}{$bm} = 1;
	}
      }
    }
  }
}

sub is_retired() {
  my ($bm) = @_;
  my $b;
  foreach $b (@retired_bm_list) {
    if ($b eq $bm) {
      return 1;
    }
  }
  return 0;
}


sub produce_html() {

  my ($jar, $bmsref) = @_;
  my $name = "$jar.html";
  my $output;
  open $output, (">$publish_html/$name");
  my $writer = XML::Writer->new(OUTPUT => $output); 
  $writer->startTag('html');
  $writer->startTag('head');
  $writer->emptyTag('link',
		    rel => "stylesheet",
		    type =>"text/css",
		    href => "dacapo.css");
  $writer->startTag('title');
  $writer->characters("Performance Results For dacapo-$jar, $id");
  $writer->endTag('title');
  $writer->endTag('head');
  $writer->startTag('body');
  $writer->startTag('font', size => -4);
  
  $writer->characters("Each graph plots performance over time for a number of JVMs. Click on graphs to enlarge, click on legend for version details on each VM.");
  $writer->emptyTag('p');
  $writer->characters("The jar used for the $id execution is ");
  $writer->startTag('a', href => "dacapo-$jar.jar");
  $writer->characters("here");
  $writer->endTag('a');
  $writer->characters(" and the sanity runs for each VM and benchmark can be view ");
  $writer->startTag('a', href => "../sanity/$id/index.html");
  $writer->characters("here");
  $writer->endTag('a');
  $writer->characters(". OS details are ");
  $writer->startTag('a', href => "os-version.txt");
  $writer->characters("here");
  $writer->endTag('a');
  $writer->characters(", and hardware details are ");
  $writer->startTag('a', href => "cpu-version.txt");
  $writer->characters("here");
  $writer->endTag('a');
  $writer->characters(".  Methodology notes appear at the end of this page.");
   
  $writer->emptyTag('p');
  do_vm_legend_html($writer);

  $writer->emptyTag('p');
  $writer->emptyTag('p');

  my $small = "_small.png";
  my $large = ".png";

  $writer->startTag('table',
		   border => 0);
  $writer->startTag('tr');

  $writer->emptyTag('th');
  do_cell($writer, 'th', "png/iteration_1_name$small",);
  do_cell($writer, 'th', "png/iteration_3_name$small",);
  do_cell($writer, 'th', "png/iteration_10_name$small",);
  do_cell($writer, 'th', "png/warmup_name$small",);

  $writer->endTag('tr');

  my $bm;
  foreach $bm (@$bmsref) { 
    my $pre = $jar."_".$bm."_";
    $writer->startTag('tr');
 
    do_cell($writer, 'td', "png/".$pre."name$small",$bm);
    do_cell($writer, 'td', "png/".$pre."1$small", $bm, "png/".$pre."1$large");
    do_cell($writer, 'td', "png/".$pre."3$small", $bm, "png/".$pre."3$large");
    do_cell($writer, 'td', "png/".$pre."10$small", $bm, "png/".$pre."10$large");
    do_cell($writer, 'td', "png/".$pre."warmup$small", $bm, "png/".$pre."warmup$large");

    $writer->endTag('tr');
  }

  $writer->endTag('table');
  $writer->characters("Methodology: Every 12 hours or so, the DaCapo suite is checked out from mercurial (hg) and built, as are a number of JVMs. After correctness testing (reported elsewhere), each JVM (both those built and those binary-released) executes each of the benchmarks in the latest DaCapo release and each of the benchmarks in the hg head.  Each benchmark is run for 10 iterations to allow for warm-up of the JVM.   In the graphs on this page we report times for the 1st, 3rd and 10th iterations.  To minimize bias due to systematic disturbance, the JVMs are iterated in the inner loop.  Once all benchmarks in both the release and hg versions of DaCapo have beenn run, a time check is made, and if time remains in the 12 hour period, another iteration of performance tests are run.   Generally we perform about 4 or 5 complete runs in a 12 hour period.  The graphs below show a dot for each of the 4 or 5 results at each time period, and plot the mean of the results for a given time period. It is beyond the scope of these experiments to perform cross-JVM heap size sensitivy comparisons, so all benchmarks are run with the same minimum and maximum heap sizes for all VMs (click on the VM name in the legend at top to see the command-line arguments used).");
  $writer->endTag('font');
  $writer->endTag('body');
  $writer->endTag('html');
  close $output;
}

sub do_vm_legend_html() {
  my ($writer) = @_;
  my $vm;
  foreach $vm (sort @vms) {
    my $img = "png/$vm"."_legend.png";
    my $ref = "$vm-version.txt";
    $writer->startTag('a',
		      href => $ref);
    $writer->emptyTag('img',
		      src => $img,
		      align => 'center',
		      border => 0,
		      alt => $vm);
    $writer->endTag('a');
  }
}

sub do_cell() {
  my ($writer, $type, $main, $bm, $secondary) = @_;
  $writer->startTag($type);
  if ($secondary ne "") {
    $writer->startTag('a',
		      href => $secondary);
  }
  $writer->emptyTag('img',
		    src => $main,
		    align => 'right',
		    border => 0,
		    alt => $bm);
  if ($secondary ne "") {
    $writer->endTag('a');
  }
  $writer->endTag($type);
}

sub produce_bm_name() {
  my ($bm, $jar) = @_;

  my $text_width = int( 0.1 * $image_width);
  my $main_font_px = int( 0.75 * $text_width);
  my $sub_font_px =  int( 0.25 * $text_width);
  my $main_offset = int(0.85 * $main_font_px);
  my $sub_offset = int(0.95 * $text_width);
  my $output;
  my $name = $jar."_".$bm."_name.svg";
  open $output, (">$root_dir/$svg_path/$name");
  my $writer = XML::Writer->new(OUTPUT => $output); 
  $writer->xmlDecl('UTF-8');
  $writer->pi('xml-stylesheet', 'href="../bin/plot.css" type="text/css"');
  $writer->doctype('svg', '-//W3C//DTD SVG 20001102//EN',
'http://www.w3.org/TR/2000/CR-SVG-20001102/DTD/svg-20001102.dtd');
  $writer->startTag('svg',
                   height => $image_height,
                   width  => $text_width,
		   xmlns => "http://www.w3.org/2000/svg");

  $writer->emptyTag('rect',
		    height => $image_height,
		    width  => $text_width,
		    fill   => "white");

#  my $basefont = "font-family:$font_family;text-anchor:middle;font-size:";
  my $basefont = "text-anchor:middle;font-weight:bold;font-size:";

  my $font = $basefont.$main_font_px."px";
  $writer->startTag('text',
		    transform => " translate($main_offset,".(int($image_height/2)).")  rotate(-90)",
		    style => $font,
		    fill => "black");
  $writer->characters($bm);
  $writer->endTag('text');

  $font = $basefont.$sub_font_px."px";#;font-style:italic";
  $writer->startTag('text',
		    transform => " translate($sub_offset,".(int($image_height/2)).")  rotate(-90)",
		    style => $font,
		    fill => "black");
  $writer->characters("dacapo-".$jar);
  $writer->endTag('text');

   $writer->endTag('svg');
  $writer->end();
  close $output;
}

sub produce_column_name() {
  my ($filename, $text) = @_;

  my $text_height = int( 0.08 * $image_width);
  my $main_font_px = int( 0.6 * $text_height);
  my $sub_font_px =  int( 0.25 * $text_height);
  my $main_offset = int(0.85 * $main_font_px);
  my $sub_offset = int(0.95 * $text_height);
  my $output;
  open $output, (">$root_dir/$svg_path/$filename");
  my $writer = XML::Writer->new(OUTPUT => $output); 
  $writer->xmlDecl('UTF-8');
  $writer->pi('xml-stylesheet', 'href="../bin/plot.css" type="text/css"');
  $writer->doctype('svg', '-//W3C//DTD SVG 20001102//EN',
'http://www.w3.org/TR/2000/CR-SVG-20001102/DTD/svg-20001102.dtd');
  $writer->startTag('svg',
                   height => $text_height,
                   width  => $image_width,
		   xmlns => "http://www.w3.org/2000/svg");


  $writer->emptyTag('rect',
		    height => $text_height,
		    width  => $image_width,
		    fill   => "white");


  my $basefont = "text-anchor:middle;font-weight:bold;font-size:";

  my $font = $basefont.$main_font_px."px";
  $writer->startTag('text',
		    transform => " translate(".(int($image_width/2)).",$main_offset) ",
		    style => $font,
		    fill => "black");
  $writer->characters($text);
  $writer->endTag('text');

   $writer->endTag('svg');
  $writer->end();
  close $output;
}


sub produce_vm_legend() {
  my ($vm) = @_;

  my $legend_width = int (0.15 * $image_width);
  my $font_size = 0.8 * $font_height;
  
  my $font_pix = int($plot_height * $font_size);
  my $font = "text-anchor:end;font-size:".$font_pix."px";

  my $legend_height = int (1.2 * $font_pix);

  my $output;
  my $name = $vm."_legend.svg";
  open $output, (">$root_dir/$svg_path/$name");
#  print "-->$name\n";
  my $writer = XML::Writer->new(OUTPUT => $output); 
  $writer->xmlDecl('UTF-8');
  $writer->pi('xml-stylesheet', 'href="../bin/plot.css" type="text/css"');
  $writer->doctype('svg', '-//W3C//DTD SVG 20001102//EN',
'http://www.w3.org/TR/2000/CR-SVG-20001102/DTD/svg-20001102.dtd');
  $writer->startTag('svg',
                   height => $legend_height,
                   width  => $legend_width,
		   xmlns => "http://www.w3.org/2000/svg");


  $writer->emptyTag('rect',
		    height => $legend_height,
		    width  => $legend_width,
		    fill   => "white");


  do_legend($writer, $legend_width, 0, $legend_width, $font, $font_pix, $vm);

   $writer->endTag('svg');
  $writer->end();
  close $output;
}


sub plot_graphs() {
  my ($bm, $jar, $max_hour_id) = @_;
#  print "===>$max_hour_id\n";

  my @min;
  my %linedata;
  my $vm;
  foreach $vm (@vms) {
    get_line_data("$root_dir/$csv_path/$vm/perf_".$jar."_".$bm."_default.csv", \%{$linedata{$vm}}, \@min);
  }
  my $iteration;
  my $iterations = scalar(@min);
  my $globalmin;
  $globalmin = $min[0];
  my $value;
  foreach $value (@min) {
    if ($value < $globalmin) {$globalmin = $value;}
  }

  my $norm;
  if (!$normalize_to_iteration) {
    $norm = $globalmin;
  }

  for ($iteration = 0; $iteration < $iterations; $iteration++) {
    my $output;
    my $name = $jar."_".$bm."_".($iteration+1).".svg";
    open $output, (">$root_dir/$svg_path/$name");
    my $writer = XML::Writer->new(OUTPUT => $output);
    start_plot_canvas($writer);
    do_plot_background($writer);
    do_axes($writer, 0);
    do_title($writer, "$bm", "dacapo-$jar, iteration ".($iteration+1));
    if ($normalize_to_iteration) {
      $norm = $min[$iteration];
    }
#    print "$bm $iteration $norm\n";
    foreach $vm (sort @vms) {
      do_line_points($writer, \%{$linedata{$vm}}, $norm, $iterations, $iteration, $vm_color{$vm});
    }
    my $i = 0;
    foreach $vm (sort @vms) {
      do_line_mean($writer, \%{$linedata{$vm}}, $norm, $iterations, $iteration, $vm_color{$vm});
      do_mainlegend($writer, $vm, $i, scalar(@vms));
      $i++;
    }
    finish_plot_canvas($writer);

    close $output;
  }

  my $output;
  my $name = $jar."_".$bm."_warmup.svg";
  open $output, (">$root_dir/$svg_path/$name");
  my $writer = XML::Writer->new(OUTPUT => $output);
  start_plot_canvas($writer);
  do_plot_background($writer);
  do_axes($writer, 1);
  my $name = $id;
  $name =~ s/_.*$//;
  do_title($writer, "$bm", "dacapo-$jar, warmup, $name");
  my $i = 0;
  $norm = $globalmin;
  foreach $vm (sort @vms) {
    do_line_points_warmup($writer, \%{$linedata{$vm}}, $norm, $iterations, $max_hour_id, $vm_color{$vm});
    do_line_mean_warmup($writer, \%{$linedata{$vm}}, $norm, $iterations, $max_hour_id, $vm_color{$vm});
    do_mainlegend($writer, $vm, $i, scalar(@vms));
    $i++;     
  }
  finish_plot_canvas($writer);
}



sub start_plot_canvas() {
  my ($writer) = @_;

$writer->xmlDecl('UTF-8');
$writer->doctype('svg', '-//W3C//DTD SVG 20001102//EN',
'http://www.w3.org/TR/2000/CR-SVG-20001102/DTD/svg-20001102.dtd');
 $writer->pi('xml-stylesheet', 'href="../bin/plot.css" type="text/css"');
$writer->startTag('svg',
                   height => $image_height,
                   width  => $image_width,
		   xmlns => "http://www.w3.org/2000/svg");

$writer->emptyTag('rect',
                   height => $image_height,
                   width  => $image_width,
                   fill   => "white");

 $writer->startTag('g',
		    id => 'plot',
		    transform => 'translate('.$plot_x_offset.','.$plot_y_offset.')',
#		    style => 'font-size:42;font-weight:bold;'
		   );


}

sub do_plot_background() {
  my ($writer) = @_;
#  my $gray = "#f0f0f0";
  my $gray = "#e0e0e0";

  for (my $y = 0; $y < 5; $y++) {
    $writer->emptyTag('rect',
		      x => 0,
		      y => (($plot_height/10) * 2* $y), 
		      height => ($plot_height/10),
		      width => $plot_width,
		      fill => $gray);
  }
  for (my $y = 0; $y < 10; $y++) {
    my $yval = (($plot_height/10) * (.5 + $y));
    my $color = ($y % 2 == 1) ? $gray : "white";
    $writer->emptyTag('line',
		      x1 => 0,
		      y1 => $yval, 
		      x2 => $plot_width,
		      y2 => $yval,
		      stroke => $color,
		      opacity => 0.5);
  }
}

sub do_title() {
  my ($writer, $title, $subtitle) = @_;
  my $font_height = 0.06;
  my $sub_font_height = 0.03;
  my $yoffset = int(-0.007 * $plot_height);
  my $x = int ($plot_width/2);
  my $base_font = "text-anchor:middle;";
 
  my $font_px = int ($sub_font_height * $plot_height);
  my $y = $yoffset + int (-$font_px/2);
  my $font = $base_font."font-size:".$font_px."px;font-style:italic";
  $writer->startTag('text',
		    transform => "translate($x, $y)",
		    style => $font,
#		    fill => "#808080"
		    fill => "black"
		   );
  $writer->characters($subtitle);
  $writer->endTag('text');
  $y -= int(1.5*$font_px);

  $font_px = int ($font_height * $plot_height);
  $font = $base_font."font-size:".$font_px."px;font-weight:bold";
#  $font = $base_font."font-size:".$font_px."px";
  $writer->startTag('text',
		    transform => "translate($x, $y)",
		    style => $font,
		    fill => "black");
  $writer->characters($title);
  $writer->endTag('text');
}

sub do_mainlegend() {
  my ($writer, $vm, $num, $tot) = @_;
  my $font_size = .8*$font_height;
  my $yoffset = 0.08 + $font_size;
  my $rows = 2;
  my $cols = int(($tot+1)/$rows);

  my $font_pix = int($plot_height * $font_size);
  my $font = "text-anchor:end;font-size:".$font_pix."px";

  my $legend_width = int($plot_width/$cols);
  my $col = int($num/$rows);
  my $row = ($num % $rows);
  my $xright = ($col +1) * $legend_width;

  my $y = $plot_height + int(($yoffset + (($row == 1) ? $font_size : 0))* $plot_height);

  do_legend($writer, $xright, $y, $legend_width, $font, $font_pix, $vm);
}

sub do_legend() {
  my ($writer, $xright, $y, $legend_width, $font, $font_pix, $vm) = @_;

  my $title = $vm_str{$vm};
  my $color = $vm_color{$vm};
  my $ticksize = 0.1;
  my $margin = 0.05;
  my $space = $margin/2;
  my $tick_start = int($xright - ($legend_width * ($ticksize + $margin)));
  my $tick_width = int($legend_width * $ticksize);
  my $text_right = int($tick_start - ($legend_width * $space));
  my $xleft = $xright - $legend_width;



  $y += $font_pix;
  $writer->startTag('text',
		    transform => "translate($text_right, $y)",
		    style => $font,
		    fill => "black"
		   );
  $writer->characters($title);
  $writer->endTag('text');
  $y -= int($font_pix/3);
  $writer->emptyTag('line',
		    x1 => $tick_start,
		    y1 => $y, 
		    x2 => ($tick_start + $tick_width),
		    y2 => $y,
		    'stroke-width' => $line_stroke,
		    stroke => $color);  

}

sub do_axes() {
  my ($writer, $warmup) = @_;


#  my $color = "#808080";
  my $color = "black";
  my $tick_px = int ($tick_height * $plot_height);
  my $font_px = int ($font_height * $plot_height);
  my $font = "font-size:".$font_px."px;text-anchor:middle";
  my $lablefont = $font.";font-style:italic";

  if ($warmup == 1) {
    do_x_axis_warmup($writer, $color, $tick_px, $font_px, $font, $lablefont);
  } else {
    do_x_axis($writer, $color, $tick_px, $font_px, $font, $lablefont);
  }
  do_y_axis($writer, $color, $tick_px, $font_px, $font, $lablefont);
}

sub do_x_axis() {
  my ($writer, $color, $tick_px, $font_px, $font, $lablefont) = @_;
  my $day_hours = 24;
  my $week_hours = $day_hours * 7;
  my $month_hours = ($day_hours * 356) / 12; 
  my $ago = 0;
  my %hours = ('day' => 24, 'week' => (24 * 7), 'month' => int ((24 * 356) / 12));
  my %limit = ('day' => (24 * 7), 'week' => int ((24 * 356) / 12), 'month' => int ((10 * 24 * 356)));

  my ($x, $y1, $y2, $yt, $yt2);
  $y1 = $plot_height;
  $y2 = $plot_height + $tick_px;
  $yt = int($y2 + (1.0*$font_px));
  $yt2 = int($yt  + (1.0*$font_px));
  $writer->emptyTag('line',
		    x1 => 0,
		    y1 => $plot_height, 
		    x2 => $plot_width,
		    y2 => $plot_height,
		    stroke => $color);  
  my $period;
  foreach $period ("day", "week", "month") {
    my $period_hours = $hours{$period};
    my $limit_hours = $limit{$period};
    if ($period_hours >= $max_hour_id) { last; }
    if ($limit_hours > $max_hour_id) { $limit_hours = $max_hour_id; }
    do_x_axis_period($writer, $period, $period_hours, $limit_hours, $color, $font, $lablefont, $y1, $y2, $yt, $yt2);
  }
}

sub do_x_axis_warmup() {
  my ($writer, $color, $tick_px, $font_px, $font, $lablefont) = @_;

  my ($x, $y1, $y2, $yt, $yt2);
  $y1 = $plot_height;
  $y2 = $plot_height + $tick_px;
  $yt = int($y2 + (1.0*$font_px));
  $yt2 = int($yt  + (1.0*$font_px));
  $writer->emptyTag('line',
		    x1 => 0,
		    y1 => $plot_height, 
		    x2 => $plot_width,
		    y2 => $plot_height,
		    stroke => $color);  
  my $iteration;
  for ($iteration = 0; $iteration < 10; $iteration++) {
    $x = $iteration * ($plot_width/9);
    $writer->emptyTag('line',
		      x1 => $x,
		      y1 => $y1, 
		      x2 => $x,
		      y2 => $y2,
		      stroke => $color);
    $writer->startTag('text',
		      transform => "translate($x, $yt)",
		      style => $font,
		      fill => $color);
    $writer->characters(($iteration+1));
    $writer->endTag('text');
  }
  $writer->startTag('text',
		    transform => "translate(".($plot_width/2).", $yt2)",
		    style => $lablefont,
		    fill => $color);
  $writer->characters("Iteration");
  $writer->endTag('text');
}

sub do_x_axis_period() {
  my ($writer, $period, $period_hours, $limit_hours, $color, $font, $lablefont, $y1, $y2, $yt, $yt2) = @_;
   
  my $ago;
  my $x;
  my $start = ($period eq "day") ? 0 : $period_hours;
  for ($ago = $start; $ago < $limit_hours; $ago += $period_hours) {
    $x = hours_ago_to_x($ago);
    $writer->emptyTag('line',
		      x1 => $x,
		      y1 => $y1, 
		      x2 => $x,
		      y2 => $y2,
		      stroke => $color);
    my $count = $ago/$period_hours;
    $writer->startTag('text',
		      transform => "translate($x, $yt)",
		      style => $font,
		      fill => $color);
    $writer->characters($count);
    $writer->endTag('text');
  }
  my $xstart = ($period eq "day") ? $plot_width : hours_ago_to_x($period_hours);
  my $xend = hours_ago_to_x($limit_hours);
  my $xcenter = $xend + (($xstart - $xend)/2);
  $writer->startTag('text',
		    transform => "translate($xcenter, $yt2)",
		    style => $lablefont,
		    fill => $color);
  my $str = $period."s ago";
  $writer->characters($str);
  $writer->endTag('text');
}

sub do_y_axis() {
  my ($writer, $color, $tick_px, $font_px, $font, $lablefont) = @_;
  my ($y, $x1, $x2, $xt, $xt2);  

  $x1 = 0;
  $x2 = $x1 - $tick_px;
  $xt = int($x2 - (1.1*$font_px));
  $xt2 = int($xt - (1.5*$font_px));
  $writer->emptyTag('line',
		    x1 => 0,
		    y1 => 0, 
		    x2 => 0,
		    y2 => $plot_height,
		    stroke => $color);
  my $y_mid = int($plot_height/2);
  $writer->startTag('text',
		    transform => "translate($xt2, $y_mid) rotate(-90)",
		    style => "$lablefont",
		    fill => $color);
  if ($normalize_to_iteration) {
    $writer->characters("Performance (normalized to iteration best)");
  } else {
    $writer->characters("Performance (normalized to best for all iterations)");
  }
  $writer->endTag('text');
  
  my $ydivs = 10;
  for (my $ydiv = 0; $ydiv <= $ydivs; $ydiv++) {
    my $val = ($ydiv/$ydivs);
    $y = $plot_height - ($val * $plot_height);
    $writer->emptyTag('line',
		      x1 => $x1,
		      y1 => $y, 
		      x2 => $x2,
		      y2 => $y,
		      stroke => $color);
    my $y_txt = $y + int($font_px/3);
    $writer->startTag('text',
		      transform => "translate($xt, $y_txt)",
		      style => "$font",
		      fill => $color);
    my $ystr = sprintf("%.1f", $val);
    $writer->characters($ystr);
    $writer->endTag('text');
  }
}

sub hours_ago_to_x() {
  my ($hours_ago) = @_;
  return $plot_width * hours_ago_to_pos($hours_ago);
}

sub finish_plot_canvas() {
  my ($writer) = @_;
  $writer->endTag('g');
  $writer->endTag('svg');
  $writer->end();
}

sub do_line_mean() {
  my($writer, $linedataref, $divisor, $iters, $iter, $color) = @_;

  my $hr_id;
  my $path;
  foreach $hr_id (sort { $a <=> $b } keys %$linedataref) {
    my $set = $$linedataref{$hr_id};
    my $x = int($plot_width*hour_id_to_pos($hr_id));
    if ($set) {
#      print "->$hr_id<-";
      my $s;
      my $mean = 0;
      my $skipped = 0;
      for ($s = 0; $s < @$set; $s++) {
	if (@{$$set[$s]} != $iters) { $skipped++; next; }
	$mean += $$set[$s][$iter];
      }
      if ($mean == 0) { next; }
      $mean = $mean/(@$set - $skipped);
      my $y = $plot_height - int($plot_height*($divisor/$mean));
      if ($path) {
	$path .= "L $x,$y ";
      } else {
	$path = "M $x,$y ";
      }
    }
  }
#  my $blur = "filter:url(#Gaussian_Blur)";
  $writer->emptyTag('path',
		    d => $path,
		    fill => 'none',
		    stroke => "#000000",
		    'stroke-width' => (int(1.5*$line_stroke)),
		    opacity => .2,
		    transform => "translate(2, 2)",
#		   style => $blur
		   );
  $writer->emptyTag('path',
		    d => $path,
		    fill => 'none',
		    stroke => $color,
		    'stroke-width' => $line_stroke);

}

sub do_line_mean_warmup() {
  my($writer, $linedataref, $divisor, $iters, $hour_id, $color) = @_;

  my $path;
  my $set = $$linedataref{$hour_id};
  if ($set) {
    my $iteration;
    for ($iteration = 0; $iteration < $iters; $iteration++) {
      my $x = int($iteration * ($plot_width/($iters-1)));
      my $s;
      my $mean = 0;
      my $skipped = 0;
      for ($s = 0; $s < @$set; $s++) {
	if (@{$$set[$s]} != $iters) { $skipped++; next; }
	$mean += $$set[$s][$iteration];
      }
      if ($mean == 0) { next; }
      $mean = $mean/(@$set - $skipped);
      my $y = $plot_height - int($plot_height*($divisor/$mean));
      if ($path) {
	$path .= "L $x,$y ";
      } else {
	$path = "M $x,$y ";
      }
    }
    
    $writer->emptyTag('path',
		      d => $path,
		      fill => 'none',
		      stroke => "#000000",
		      'stroke-width' => (int(1.5*$line_stroke)),
		      opacity => .2,
		      transform => "translate(2, 2)",
		      #		   style => $blur
		     );
    $writer->emptyTag('path',
		    d => $path,
		      fill => 'none',
		      stroke => $color,
		      'stroke-width' => $line_stroke);
  }
}
sub do_line_points_warmup() {
  my($writer, $linedataref, $divisor, $iters, $hour_id, $color) = @_;


  my $set = $$linedataref{$hour_id};
  if ($set) {
    my $iteration;
    for ($iteration = 0; $iteration < $iters; $iteration++) {
      my $x = int($iteration * ($plot_width/($iters-1)));
      my $s;
      for ($s = 0; $s < @$set; $s++) {
	if (@{$$set[$s]} != $iters) { next; }
	my $y = $plot_height- int($plot_height*($divisor/($$set[$s][$iteration])));
	$writer->emptyTag('circle',
			  cx => $x,
			  cy => $y,
			  r => 2,
			  opacity => .2,
			  fill => $color);
      }
    }
  }
}

sub do_line_points() {
  my($writer, $linedataref, $divisor, $iters, $iter, $color) = @_;

  my $hr_id;
  foreach $hr_id (sort keys %$linedataref) {
    my $set = $$linedataref{$hr_id};
    my $x = int($plot_width*hour_id_to_pos($hr_id));
    if ($set) {
      my $s;
      for ($s = 0; $s < @$set; $s++) {
	if (@{$$set[$s]} != $iters) { next; }
	my $y = $plot_height- int($plot_height*($divisor/($$set[$s][$iter])));
	$writer->emptyTag('circle',
			  cx => $x,
			  cy => $y,
			  r => 2,
			  opacity => .2,
			  fill => $color);
      }
    }
  }
}


#
# Get the line data for one csv file (all iterations),
# updating the min times array as appropriate.
#
sub get_line_data() {
  my($csvfilename, $linedataref, $minref) = @_;
  my $csvfile;
  
  open $csvfile, ($csvfilename);
  while (<$csvfile>) {
    if (!/^\s*#/) {
      chomp;
      s/\s+//g;
      my ($str, $hr_id, $elapsed, @iterations) = split /,/;
      
      push @{$$linedataref{$hr_id}}, [ @iterations ];
      my $i;
      for ($i = 0; $i < @iterations; $i++) {
	if (!($$minref[$i] && ($$minref[$i] < $iterations[$i]))) {
	  $$minref[$i] = $iterations[$i];
	}
      }
    }
  }
  close $csvfile;
}

sub get_hour_id_from_time() {
  my ($time) = @_;
  my $hr_id = (int ($time/(60 * 60))) - $begining_of_time_hrs;
  return $hr_id;
}

#
# return a value from 0 to 1 (1 most recent)
#
sub hour_id_to_pos() {
  my ($hour_id) = @_;
  return hours_ago_to_pos($max_hour_id - $hour_id);
}

#
# return a value from 0 to 1 (1 most recent)
#
sub hours_ago_to_pos() {
  my ($ago) = @_;
  if (1) {
    my $foo = 40;
    my $value = $ago; # + 1;

    my $offset = 1-((log ($foo))/(log ($max_hour_id + $foo)));
    return (1-((log ($value + $foo))/(log ($max_hour_id + $foo))))/$offset;
  } else {
    my $week_hr = 7 * 24;
    my $year_hr = 365 * 24;
    my $month_hr = $year_hr/12;
    my $divisions = ($max_hour_id > $month_hr) ? 3 : ($max_hour_id > $week_hr) ? 2 : 1;

    if ($ago < $week_hr) {
      my $week_pos = ($week_hr-$ago)/$week_hr;
      return (($divisions - 1)/$divisions + ($week_pos/$divisions));
    } elsif ($ago < $month_hr) {
      my $month_pos = ($year_hr-$ago)/$year_hr;
      return (($divisions -2)/$divisions + ($month_pos/$divisions));
    } else {
      my $year_pos = ($max_hour_id-$ago)/$max_hour_id;
      return $year_pos/$divisions;
    }
  }
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

