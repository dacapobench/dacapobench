#!/usr/bin/perl

use BaseConfig;
use VMConfig;
use JarConfig;
use RunConfig;
use strict;
use XML::Writer;

my $image_height = 400;
my $image_width = 640;
my $plot_r_margin = 0.015;
my $plot_l_margin = 0.15;
my $plot_t_margin = 0.09;
my $plot_b_margin = 0.13;
my $tick_height = 0.025;
my $font_height = 0.04;
my $line_stroke = 3;
my $font_family = "verdana";

my $plot_height = int ($image_height * (1-($plot_t_margin+$plot_b_margin)));
my $plot_y_offset = int ($image_height * $plot_t_margin);
my $plot_width = int($image_width * (1-($plot_l_margin+$plot_r_margin)));
my $plot_x_offset = int ($image_height * $plot_l_margin);

my $plot_time = 1217179695;
my $max_hour_id = get_hour_id_from_time($plot_time);

my $bm;

foreach $bm (@old_bm_list) {
  plot_graphs("$bm", "2006-10-MR2");
}

sub plot_graphs() {
  my ($bm, $jar) = @_;

  my @min;
  my %linedata;
  my $vm;
  foreach $vm (@vms) {
    get_line_data("csv/$vm/perf_".$jar."_".$bm."_default.csv", \%{$linedata{$vm}}, \@min);
  }
  my $iteration;
  for ($iteration = 0; $iteration < 10; $iteration++) {
    my $output;
    my $name = $jar."_".$bm."_".($iteration+1).".svg";
    open $output, (">svg/$name");
    my $writer = XML::Writer->new(OUTPUT => $output);
    start_plot_canvas($writer);
    do_plot_background($writer);
    do_axes($writer);
    do_title($writer, "$bm", "dacapo-$jar, iteration ".($iteration+1));
    foreach $vm (@vms) {
      do_line_points($writer, \%{$linedata{$vm}}, @min[$iteration], $iteration, $vm_color{$vm});
    }
    my $i = 0;
    foreach $vm (@vms) {
      do_line_mean($writer, \%{$linedata{$vm}}, @min[$iteration], $iteration, $vm_color{$vm});
      do_legend($writer, $vm_str{$vm}, $vm_color{$vm}, $i, scalar(@vms));
      $i++;
    }
#  do_line_points($writer, \%{$linedata{$vm}}, @min[9], 9, "green");
#  do_line_mean($writer, \%{$linedata{$vm}}, @min[9], 0, "red");
#  do_line_mean($writer, \%{$linedata{$vm}}, @min[9], 9, "green");
#  do_legend($writer, "Line A", "red", 0, 2);
#  do_legend($writer, "Line B", "green", 1, 2);
    finish_plot_canvas($writer);

    close $output;
  }
}



sub start_plot_canvas() {
  my ($writer) = @_;

$writer->xmlDecl('UTF-8');
$writer->doctype('svg', '-//W3C//DTD SVG 20001102//EN',
'http://www.w3.org/TR/2000/CR-SVG-20001102/DTD/svg-20001102.dtd');
$writer->startTag('svg',
                   height => $image_height,
                   width  => $image_width,
		   xmlns => "http://www.w3.org/2000/svg");

$writer->emptyTag('rect',
                   height => $image_height,
                   width  => $image_width,
                   fill   => "white");
$writer->startTag('defs');
$writer->startTag('filter',
		  id => "Guassian_Blur");
$writer->emptyTag('feGaussianBlur',
		  in => 'SourceGraphic',
		  stdDeviation => 3);
$writer->endTag('filter');
$writer->endTag('defs');

  $writer->startTag('g',
		    id => 'plot',
		    transform => 'translate('.$plot_x_offset.','.$plot_y_offset.')',
		    style => 'font-size:42;font-weight:bold;');


}

sub do_plot_background() {
  my ($writer) = @_;
  for (my $y = 0; $y < 5; $y++) {
    $writer->emptyTag('rect',
		      x => 0,
		      y => (($plot_height/10) * 2* $y), 
		      height => ($plot_height/10),
		      width => $plot_width,
		      fill => "#f0f0f0");
  }
  for (my $y = 0; $y < 10; $y++) {
    my $yval = (($plot_height/10) * (.5 + $y));
    my $color = ($y % 2 == 1) ? "#f0f0f0" : "white";
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
  my $base_font = "font-family:$font_family;text-anchor:middle;";
 
  my $font_px = int ($sub_font_height * $plot_height);
  my $y = $yoffset + int (-$font_px/2);
  my $font = $base_font."font-size:".$font_px."px;font-style:italic";
  $writer->startTag('text',
		    transform => "translate($x, $y)",
		    style => $font,
		    fill => "#808080");
  $writer->characters($subtitle);
  $writer->endTag('text');
  $y -= int(1.5*$font_px);

  $font_px = int ($font_height * $plot_height);
  $font = $base_font."font-size:".$font_px."px";
  $writer->startTag('text',
		    transform => "translate($x, $y)",
		    style => $font,
		    fill => "black");
  $writer->characters($title);
  $writer->endTag('text');
}

sub do_legend() {
  my ($writer, $title, $color, $num, $tot) = @_;
  my $font_size = .75*$font_height;
  my $yoffset = 0.08 + $font_size;

  my $font_pix = int($plot_height * $font_size);
  my $ticksize = 0.1;
  my $margin = 0.05;
  my $font = "text-anchor:end;font-family:$font_family;font-size:".$font_pix."px";

  my $space = $margin/2;
  my $legend_width = int($plot_width/$tot);
  my $xright = ($num + 1) * $legend_width;
  my $tick_start = int($xright - ($legend_width * ($ticksize + $margin)));
  my $tick_width = int($legend_width * $ticksize);
  my $text_right = int($tick_start - ($legend_width * $space));
  my $y = $plot_height + int(($yoffset)* $plot_height);
  my $xleft = $xright - $legend_width;
  $y += $font_pix;
  $writer->startTag('text',
		    transform => "translate($text_right, $y)",
		    style => $font,
		    fill => "#808080");
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
  my ($writer) = @_;


  my $color = "#808080";
  my $tick_px = int ($tick_height * $plot_height);
  my $font_px = int ($font_height * $plot_height);
  my $font = "font-family:$font_family;font-size:".$font_px."px;text-anchor:middle";
  my $lablefont = $font.";font-style:italic";

  do_x_axis($writer, $color, $tick_px, $font_px, $font, $lablefont);
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
  $writer->characters("Performance (normalized to best)");
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
  my($writer, $linedataref, $divisor, $iter, $color) = @_;

  my $hr_id;
  my $path;
  foreach $hr_id (sort { $a <=> $b } keys %$linedataref) {
    my $set = $$linedataref{$hr_id};
    my $x = int($plot_width*hour_id_to_pos($hr_id));
    if ($set) {
      my $s;
      my $mean = 0;
      for ($s = 0; $s < @$set; $s++) {
	$mean += $$set[$s][$iter];
      }
      $mean = $mean/@$set;
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


sub do_line_points() {
  my($writer, $linedataref, $divisor, $iter, $color) = @_;

  my $hr_id;
  foreach $hr_id (sort keys %$linedataref) {
    my $set = $$linedataref{$hr_id};
    my $x = int($plot_width*hour_id_to_pos($hr_id));
    if ($set) {
      my $s;
      for ($s = 0; $s < @$set; $s++) {
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
    my $value = $ago + 1;

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
