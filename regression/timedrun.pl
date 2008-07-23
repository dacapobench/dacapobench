#!/usr/bin/perl
#
# I reserve no legal rights to this software, it is fully in the public
# domain.  Any company or individual may do whatever they wish with this
# software.
#
# Steve Blackburn May 2005
#
require "getopts.pl";
&Getopts('t:');
if ($opt_t ne "") {
    $SIG{ALRM} = 'alarmHandler';
    $deadman_seconds = $opt_t;
}

FORK: {
    if ($pid = fork) {
	# parent
	if (defined ($deadman_seconds)){
	    alarm($deadman_seconds);
	}
	wait;
    } elsif (defined $pid) { # $pid is zero here if defind
	# child
	exec @ARGV;
    } elsif ($! =~ /No more process/) {
	# EAGAIN, supposedly recoverable fork error
	sleep 5;
	redo FORK;
    } else {
	# weird fork error
	die "Can't fork: $!\n";
    }
}

sub alarmHandler {
    print "CAUGHT ALARM, TERMINATING JOB $pid\n";
    system("kill  $pid 2>/dev/null");
    sleep 2;
    euthanase($pid);
    exit(0);
}

sub euthanase {
    my ($pid) = @_;
    open (JOBS, "ps -ef | cut -c10-21 | grep $pid|");
    while (<JOBS>) {
	($target) = /(\d+)\s+\d+/;
	system("kill -9 $target 2>/dev/null");
    }
    close JOBS;
}

