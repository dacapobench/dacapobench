#!/usr/bin/perl

my $user_density = 1.5;           # over-provisioning of user set (only use 1/N)
my $ops_per_session_mean = 16;
my $ops_per_session_sd = 8;
my %op_prob = (
	       "h" => .20,	# homepage
	       "q" => .45,	# quote
	       "l" =>   0,	# login (implicit)
	       "o" =>   0,	# logout (implicit)
	       "r" => .005,	# register a completely new user
	       "a" => .06,	# display account info
	       "p" => .09,	# portfolio
	       "b" => .09,	# buy
	       "s" => .09,	# sell
	       "u" => .015	# update account profile
	      );

my $add_to_holdings = 0;            # do we update holdings when buying?
my $prob_buy_quoted_stock = 0.75;   # typically buy a stock which was quoted
my $prob_sell_quoted_stock = 0.75;  # typically sell a stock which was quoted
my $prob_quote_held_stock = 0.60;   # typically get quotes on held stock
my $prob_buy_held_stock = 0.50;     # often buy held stock

my %op_actual = ();
my $sell_deficit = 0;

my $sep = "\t";


my $maxholdings = 20;
my $stocks_mean = 1000;
my $stocks_sd = 500;
my $stocks_min = 1;
my $minbalance = 90000;
my $maxbalance = 1200000;

my %fullnames = ();
my $firstnamefile = "names-first.txt";
my $familynamefile = "names-family.txt";
my $streetnamefile = "names-street.txt";
my $regionnamefile = "names-region.txt";
my @firstnames = ();
my @lastnames = ();
my @streetnames = ();
my @regionnames = ();
my @usstates = ("AL", "AK", "AS", "AZ", "AR", "CA", "CO", "CT", "DE", "DC", "FM", "FL", "GA", "GU", "HI", "ID", "IL", "IN", "IA", "KS", "KY", "LA", "ME", "MH", "MD", "MA", "MI", "MN", "MS", "MO", "MT", "NE", "NV", "NH", "NJ", "NM", "NY", "NC", "ND", "MP", "OH", "OK", "OR", "PW", "PA", "PR", "RI", "SC", "SD", "TN", "TX", "UT", "VT", "VI", "VA", "WA", "WV", "WI", "WY");
my @streettypes = ("Street", "Street", "Street", "Avenue", "Place", "Boulevard", "Road");
my @chars=('a'..'z','A'..'Z','0'..'9','_');
my @emaildomains=("gmail.com", "gmail.com", "hotmail.com", "hotmail.com", "yahoo.com", "yahoo.com", "aol.com");

my @surplus_users = ();
my %users = ();

my $output_dir = shift(@ARGV);

my %sessions = ("tiny" => 32,
		 "small" => 128,
		 "medium" => 512,
		 "large" => 2048,
		 "huge" => 8192);
my %stockquotes = ();

init($output_dir);
my $max_users = $user_density*($sessions{"huge"});
create_users($max_users);
create_operations();

my $total_ops = 0;
foreach $key (keys %op_actual) {
  $total_ops += $op_actual{$key};
}
foreach $key (sort keys %op_actual) {
  printf "$key %.3f\n", ($op_actual{$key}/$total_ops);
}
exit;

#
#
# initialization
#
#
sub init{
  my ($output_dir) = @_;

  if ($output_dir eq "") {
    die "Need to specify an output directory!";
  } else {
    system("mkdir -p $output_dir");
  }
  init_stocks();
}


#
#
# operations
#
#



sub create_operations {
  my $output_file = "$output_dir/workload.txt";
  my @userset = ();
  get_userset(\@userset);

  open (OUTPUT, ">$output_file");
  my $header = "#$sep";
  foreach $key (sort {$sessions{$a} <=> $sessions{$b}} keys %sessions) {
    $header .= "$key: ".$sessions{$key}."$sep";
  }
  print OUTPUT $header."\n";

  my $uid;
  foreach $uid (@userset) {
    my $session = generate_session($uid);
    print OUTPUT "$session\n";
  }
  close OUTPUT;
}

# get a randomized set of $sessions users
sub get_userset {
  my ($userset) = @_;
  
  my @taken = ();
  my $low = 0;
  foreach $key (sort {$sessions{$a} <=> $sessions{$b}} keys %sessions) {
    my $high = $sessions{$key};
    my $max_uid = $high*$user_density;
    for (my $spot = $low; $spot < $high; $spot++) {
      my $u = int (rand $max_uid);
      while ($taken[$u]) {
	$u++;
	if ($u == $max_uid) { $u = 0; }
      }
      $$userset[$spot] = sprintf("%.6d",$u);
      $taken[$u] = 1;
    }
    $low = $high;
  }
}

#   my $low = 0;
#   foreach $key (sort {$sessions{$a} <=> $sessions{$b}} keys %sessions) {
#     my $high = $sessions{$key};
#     for (my $i = $low; $i < $high; $i++) {
#       my $spot = $low + int(rand ($high - $low));
#       while ($$userset[$spot] ne "") {
# 	$spot++;
# 	if ($spot == $high) {$spot = $low;}
#       }
#       $$userset[$spot] = sprintf("%.6d",$i);
#     }
#     $low = $high;
#   }


sub generate_session {
  my ($uid) = @_;
  my $session = "$uid$sep".get_password($uid);
  my %holdings = ();
#  print "$uid ".get_holdings($uid)." ";
  foreach my $h (split /, /, get_holdings($uid)) {
    my ($stk, $qty) = split / /, $h;
    $holdings{"$stk:$qty"} = 1;
  }
  my $balance = get_balance($uid);

  my $op;
  my $tail_ops; # register and update ops must come only at the end
  do {
    $op = generate_op(\$uid, \%holdings, \$balance);
    my $op_id = (substr $op, 0, 1);
    if ($op_id eq "r") { # || $op_id eq "u") {
      $tail_ops .= "\t$op";
    } elsif ($op ne "o") {
      $session .= "\t$op";
    }
  } while ($op ne "o");

  $session .= "$tail_ops";

  return $session;
}




sub generate_op {
  my ($uid, $holdings, $balance) = @_;
  
  my @quotes = ();

  my $val = (1.0+(1.0/($ops_per_session_mean))) * rand;
  my $op = "o";
  my $cumul = 0.0;
  if ($val < 1.0) {
    foreach $key (keys %op_prob) {
      $cumul += $op_prob{$key};
      if ($cumul >= $val) {
	$op = $key;
	last;
      }
    }
    my $num_holdings = keys %$holdings;
    if ($op eq "s" && $num_holdings == 0) {
      $op = "b";
      $sell_deficit++;
    } elsif ($op eq "b" &&
	     $num_holdings > 0 &&
	     $sell_deficit > 0 &&
	     (rand > .5)) {
      $op = "s";
      $sell_deficit--;
    }

    $op_actual{$op}++;

    if ($op eq "q") {
      $op .= " ".get_quote($holdings, \@quotes); 
    } elsif ($op eq "s") {
      $op .= " ".get_sell($holdings, \@quotes);
    } elsif ($op eq "b") {
      $op .= " ".get_buy($holdings, $balance, \@quotes);
    } elsif ($op eq "r") {
      $op .= " ".get_register($uid, $holdings, $balance);
    } elsif ($op eq "u") {
      $op .= " ".get_update($uid);
    }
  }
  return $op;
}


sub get_quote {
  my ($holdings, $quotes) = @_;
  my $quote = ((rand() < $prob_quote_held_stock) ? get_random_stock_from_holdings($holdings) : get_weighted_random_stock());
  push @$quotes, $quote;
  return $quote;
}

sub get_sell {
  my ($holdings, $quotes) = @_;

  my $holding = get_random_holding($holdings);
  my @tmp = split(/:/, $holding);
  if ($holding eq "") {
    my $num_holdings = keys %$holdings;
    print "FOUND NULL HOLDING IN SELL-->$num_holdings<--\n";
  }
  delete $holdings->{$holding};
  return join(' ', @tmp);
}

sub get_buy {
  my ($holdings, $balance, $quotes) = @_;
  my $stock;
  if ($#{$quotes} >= 0 && (rand() < $prob_buy_quoted_stock)) {
    $stock = $$quotes[int(rand ($#{$quotes} + 1))];
  } else {
    $stock = (rand() < $prob_buy_held_stock) ?  get_random_stock_from_holdings($holdings) : get_weighted_random_stock();
  }
  my $price = $stockquotes{$stock};
 # print "[buy $stock $price]\n";
  my $avail = $$balance * .1;
  my $max = int ($avail/$price);
  my $qty = int (rand($max));
  $$balance -= ($qty * $price);
  if ($add_to_holdings) {
    $$holdings{"$stock:$qty"} = 1;
  }
  return $stock." ".$qty;
}

sub get_register {
  my ($uid, $holdings, $balance) = @_;
  my $newuser = shift(@surplus_users);
  my @details = split(/$sep/, $users{$newuser});
  $$uid = $newuser;
  $$balance = $details[0];
  %$holdings = ();
  pop(@details); # get rid of holdings
  return $newuser."|".join('|', @details);
}

sub get_update {
  my ($uid) = @_;
  my @swappablefields = (1,2,3,5);

  my @current = split(/$sep/, $users{$$uid});
  my $newdetails = pop(@surplus_users);
  my @new = split(/$sep/, $users{$newdetails});
  my $field = $swappablefields[int (4.0*rand)];
  $current[$field] = $new[$field];
  $field = $swappablefields[int (4.0*rand)];
  $current[$field] = $new[$field];
  $users{$$uid} = join($sep, @current);
  pop(@current);   # get rid of balance
  shift(@current); # get rid of holdings
  return join('|', @current);
}

sub get_random_stock_from_holdings {
  my ($holdings) = @_;
  my $holding = get_random_holding($holdings);
  if ($holding eq "") {
    return get_weighted_random_stock();
  } else {
    my @tmp = split(/:/, $holding);
    return $tmp[0];
  }
}

sub get_random_holding {
  my ($holdings) = @_;
  my @held = keys %$holdings;
  if (@held == ()) {
    return "";
  } else {
    return $held[rand ($#held + 1)];
  }
}

#
#
# stocks
#
#
my @stocks = ();
my %stocktable = ();   # table of symbols from which operations are randomly drawn (weighted by popularity of stocks)

sub get_weighted_random_stock {
  my $pop = rand 1;
  my $stock;
  do {
    $stock = $stocks[int(rand $#stocks + 1)];
  } while ($stocktable{$stock} < $pop);
  #    print "$stock $pop ".$stocktable{$stock}."\n";
  return $stock;
}

sub init_stocks {
  my $stockfile = "stocks.txt";
  my $maxvolume = 0;
  my %stockqty = ();
  my $stockcount = 0;

  system("cp -f $stockfile $output_dir");

  open (STOCKS, "$stockfile");
  while (<STOCKS>) {
    chomp;
    my ($symbol, $name, $quote, $cap) = split(/\t/);
    $stockquotes{$symbol} = $quote;
    push @stocks, $symbol;
    $_ = $cap;
    my ($val, $scale) = /(.+)([MB])/;
    if ($scale == "B") {
      $val *= 10000;
    }
    my $num = int($val/$quote);
    if ($num > $maxvolume) {
      $maxvolume = $num;
    }
    $stockqty{$symbol} = $num;
    $stockcount++;
  }
  close STOCKS;
  my $rank = 1;
  foreach $s (sort {$stockqty{$a} <=> $stockqty{$b}} keys %stockqty) {
    my $val = $rank/$stockcount;
    $stocktable{$s} = $val;
    #	print "$rank $s $val\n";
    $rank++;
  }
}

#
#
# users
#
#
sub create_users {
  my ($maxusers) = @_;

  my $installed_users = 0;
  foreach $key (keys %sessions) {
    if ($sessions{$key}*$user_density > $installed_users) {
      $installed_users = $sessions{$key}*$user_density;
    }
  }

  my $surplus_users = $installed_users; # generate surplus for use in update and addition ops

  init_names();
  create_unique_users($installed_users+$surplus_users);
  write_user_table($installed_users);
  for ($u = $installed_users; $u < $installed_users+$surplus_users; $u++) {
    push @surplus_users, sprintf("%.6d", $u);
  }
}

sub write_user_table {
  my ($installed_users) = @_;

  my $usersfile = "$output_dir/users.txt";
  open (USERS, ">$usersfile");
  
  my $header = "#$sep";
  foreach $key (sort {$sessions{$a} <=> $sessions{$b}} keys %sessions) {
    $header .= "$key: ".($sessions{$key}*$user_density)."$sep";
  }
  print USERS $header."\n";

  for ($u = 0; $u < $installed_users; $u++) {
    my $uid = sprintf("%.6d", $u);
    print USERS "$uid$sep".$users{$uid}."\n";
  }
  close (USERS);  
}

sub init_names {

  open (FIRST, "$firstnamefile");
  while (<FIRST>) {
    chomp;
    push @firstnames, $_;
  }
  close FIRST;

  open (LAST, "$familynamefile");
  while (<LAST>) {
    chomp;
    push @lastnames, $_;
  }
  close LAST;

  open (STREET, "$streetnamefile");
  while (<STREET>) {
    chomp;
    push @streetnames, $_;
  }
  close STREET;

  open (REGION, "$regionnamefile");
  while (<REGION>) {
    chomp;
    push @regionnames, $_;
  }
  close REGION;
}


sub create_unique_users {
  my ($maxusers) = @_;

  for ($u = 0; $u < $maxusers; $u++) {
    my $uid = sprintf("%.6d", $u);

    my $credit = sprintf("%.3d-%.4d-%.4d-%.4d", int(rand 1000), int(rand 10000), int(rand 10000), int(rand 10000));

    my $fullname; my $first; my $last;
    do {
      $first = $firstnames[int(rand $#firstnames)];
      $last = $lastnames[int(rand $#lastnames)];
      $fullname = "first: $first last: $last";
    } while ($fullnames{$fullname} ne "");
    $fullnames{$fullname} = 1;

    my $balance = $minbalance + int(rand ($maxbalance - $minbalance));

    my $number = 1+int(rand 100);
    my $street = $streetnames[int(rand $#streetnames+1)]." ".$streettypes[int(rand $#streettypes+1)];
    my $city = $regionnames[int(rand $#regionnames+1)];
    my $zip = sprintf("%.5d", int(rand 100000));
    my $state = $usstates[int(rand $#usstates+1)];
    my $address = "$number $street, $city, $zip, $state";

    my $password = "";
    for ($i = 0; $i < 8; $i++) {
      $password .= $chars[int(rand $#chars+1)];
    }

    my $email = "$first.$last\@".$emaildomains[int(rand $#emaildomains+1)];
    
    my $holdingscount = 1+int(rand (rand $maxholdings));
    my $hold = "";
    for ($i = 0; $i < $holdingscount; $i++) {
      $stock = get_weighted_random_stock();
      my $qty = int($stocks_mean+(gaussian_rand()*$stocks_sd));
      if ($qty < 10*$stocks_min) {
	$qty = $stocks_min + int(rand(9*$stocks_min));
      }
      if ($i != 0) {
	$hold .= ", ";
      }
      $hold .= "$stock $qty";
    }

    my $record = "$balance$sep$credit$sep$email$sep$password$sep$fullname$sep$address$sep$hold";
    $users{$uid} = $record;
  }
}


sub get_balance {
  my ($uid) = @_;
  my @fields = split /\t/, $users{$uid};
  return $fields[0];
}
sub get_password {
  my ($uid) = @_;
  my @fields = split /\t/, $users{$uid};
  return $fields[3];
}
sub get_holdings {
  my ($uid) = @_;
  my @fields = split /\t/, $users{$uid};
  return $fields[6];
}

sub gaussian_rand {
    my ($u1, $u2);  # uniformly distributed random numbers
    my $w;          # variance, then a weight
    my ($g1, $g2);  # gaussian-distributed numbers

    do {
        $u1 = 2 * rand() - 1;
        $u2 = 2 * rand() - 1;
        $w = $u1*$u1 + $u2*$u2;
    } while ( $w >= 1 );

    $w = sqrt( (-2 * log($w))  / $w );
    $g2 = $u1 * $w;
    $g1 = $u2 * $w;
    # return both if wanted, else just one
    return wantarray ? ($g1, $g2) : $g1;
}
