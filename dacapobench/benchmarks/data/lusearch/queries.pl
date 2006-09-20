#!/usr/bin/perl
$frac = shift(@ARGV);
@allwords = ();
while(<STDIN>) { push @allwords, split /\s+/; }

@result = keep_n($frac, @allwords);
shuffle(*result);
foreach $f (@result) { print "$f\n"; }

#
# Shuffle the elements of an array
#
sub shuffle {
    my $array = shift;
    my $i;
    for ($i = @$array; --$i; ) {
        my $j = int rand ($i+1);
        next if $i == $j;
        @$array[$i,$j] = @$array[$j,$i];
    }
}

#
# To keep n% of a list
#
sub keep_n {
    my $frac = shift;
    return grep { rand() < $frac } @_;
}

