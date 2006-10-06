#!/usr/bin/perl
#$source = shift(@ARGV);        # source of words
$listorder = shift(@ARGV);          # log_2 of the number of output files
$lists = 2**$listorder;             # number of output files
$wordorder = shift(@ARGV);     # total number of words to be used
$totalwords = 2**$wordorder;
$listsizeorder = $wordorder - $listorder; 
$listsize = 2**$listsizeorder;
@allwords = ();                 # input array of words
@listarray = ();
$baseoutname = "query";
$wordlist = "";

print "creating $lists lists, each with $listsize words, for a total of ".($lists*$listsize)." words\n";

getallwords(*allwords, "words.txt.gz");
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

