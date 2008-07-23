#!/usr/bin/perl
use BaseConfig;
use VMConfig;
use JarConfig;
use RunConfig;

my $regression_period_hrs = pop @ARGV;
if ($regression_period_hrs < 1 || $regression_period_hrs > 48) {
  die "Usage: ./continuous <period>\n(where 0 < period < 49)"; 
}

my $start_time = time;
my $regression_period_sec = ($regression_period_hrs * 60 * 60);
my $target_finish = $start_time + $regression_period_sec;

while() {
  my $hour_id = (int $start_time/(60 * 60)) - $begining_of_time_hrs;
  my $id_str = get_id($start_time);

  system("./regression.pl $id_str $hour_id $target_finish");

  $target_finish += $regression_period_sec;
  $start_time = time;
}

#
# Create a string that uniquely identifies this run
#
sub get_id() {
  my ($time) = @_;
  ($sec,$min,$hour,$mday,$mon,$year,$wday,$yday,$isdst) = localtime($time);
  $year += 1900;
  $mon += 1;
  $day = (Sun,Mon,Tue,Wed,Thu,Fri,Sat)[$wday];
  $id = sprintf("%4d-%2.2d-%2.2d-%s-%2.2d-%2.2d", $year, $mon, $mday, $day, $hour, $min);
  return $id;
}


