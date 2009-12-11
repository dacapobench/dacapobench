#!/usr/bin/perl
# (re)create the queries used by lusearch
#
# This code takes a large word list, randomizes the order, then creates N query
# lists, where 1/2 of each list contains unique words (not used in any other
# query), 1/4 of each list contains words shared with one other query, 1/8
# contains words shared by three other lists, 1/16 contains words shared 8 ways,
# etc etc.
# 
# The lists are created from the randomized source using a simple algorithm, then
# the order of each list is randomized and the relationship between the lists is
# randomized.
# 
# usage: create-queries.pl N M
#    where N = log_2 the number of query lists to create
#          M = log_2 the total number of words
#
# by defualt, we use:
#        create-queries.pl 6 18
#          N = 6 => 64 query lists
#          M = 18 => 256K words
#

$wordlist = "words.txt.gz";               # source of words we're using (gleaned from kjv & shakespeare)
$baseoutname = "query";                   # basename of generated files

$listorder = shift(@ARGV);                # log_2 of the number of output files
$lists = 2**$listorder;                   # number of output files
$wordorder = shift(@ARGV);                # log_2 the number of words to be used
$totalwords = 2**$wordorder;              # number of words to be used

$listsizeorder = $wordorder - $listorder; # log_2 the length of each query list
$listsize = 2**$listsizeorder;            # length of each query list
@allwords = ();                           # input array of words
@listarray = ();                          # list of lists

print "creating $lists lists, each with $listsize words, for a total of ".($lists*$listsize)." words\n";

getallwords(*allwords, $wordlist);
populatelists(*listarray, *allwords, $listsizeorder, $lists, $listorder);
shufflelists(*listarray);
writelists(*listarray, $lists);

#
# get all the words from the source
#
sub getallwords {
  my $aw = shift;
  my $wordfile = shift;
  open (WORDSIN, "zcat $wordfile|");
  while(<WORDSIN>) { push @$aw, split /\s+/; }
  shuffle($aw);
  close(WORDSIN);
}

#
# put data in the lists
#
sub populatelists {
  my $listarray = shift;
  my $words = shift;
  my $listsizeorder = shift;
  my $lists = shift;
  my $order = shift;
  
  my $value = 0;
  my $elementbase = 0;  
  my $word = 0;
  for ($sharinglevel=0; $sharinglevel <= $listsizeorder; $sharinglevel++) {
 	my $elements = 2**($listsizeorder-($sharinglevel+1));
 	my $groups = $lists/(2**$sharinglevel);
 	my $groupsize = $lists/$groups;
 	for ($group = 0; $group < $groups; $group++) {
 	  for ($e = 0; $e < $elements; $e++) {
 	    $word  = pop(@$words);
 	    $element = $elementbase + $e;
 	    for ($gl = 0; $gl < $groupsize; $gl++) {
 	      $list = $gl + ($groupsize * $group);
 	      $listarray->[$list][$element] = $word;
 	    }
 	  }
 	}
 	$elementbase += $elements;
  }
}

#
# output to files
#
sub writelists {
   my $listarray = shift;
   my $lists = shift;
   
   my @index= ();
   for ($i = 0; $i < $lists; $i++) {
    push @index, $i;
   }
   shuffle(\@index);
   
   for ($l=0; $l < $lists; $l++) {
     $name = sprintf("%s%.*d.txt",$baseoutname,($lists < 10 ? 1 : ($lists < 100 ? 2 : 3)),$l);
	 open(OUT, ">$name");
	 for ($i=0; $i < $listsize; $i++) {
	 	print OUT ($listarray->[$index[$l]][$i]."\n");
	 }
	 close(OUT);
   }
}

#
# shuffle the lists.  First suffle the columns then shuffle each row.
#
sub shufflelists {
  my $listarray = shift;
  for ($i = 0; $i < $lists; $i++) {
  	shuffle($listarray->[$i]);
  }
}

#
# Shuffle the elements of an array
#
sub shuffle {
    my $array = shift;
    my $i;
    for ($i = @$array; --$i; ) {
        my $j = int rand ($i+1);
 #       print "<$i $j>\n";
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

