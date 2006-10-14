<?php
$includespath = "/data/web/data/feit/_includes/";
$config = $includespath."branchConfig_CS.inc";
$nav    = $includespath."siteNav.inc";
include ("$config");
$searchBar = "0";
$p=$_GET['p'];
?>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html lang="en" dir="ltr">
<head>
  <meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
  <title>ANU - <? print ($BranchTitle) ?> - <? print ($BranchAbbrev) ?></title>
  <link rel="shortcut icon" href="<? print $imagespath."anu.ico" ?>" TYPE="image/x-icon">
  <link href="http://styles.anu.edu.au/anu_global_styles.css" rel="stylesheet" type="text/css">
<style>
TABLE { border-collapse: collapse;
}
*#SIZE { border-right: 2px solid black; }
TR { border=none; }
TR.heading { border: 2px solid black; }
TD { border: 1px solid white; 
     text-align: center;
}
TD.pass { background-color: lightgreen; }
TD.fail { background-color: red; }
TD.nolog { background-color: white; }
TD.unknown { background-color: yellow; }
COL#last { border-right: 2px solid black; }
</style>
<!-- BEGIN DOCUMENT META DATA -->
  <meta name="keywords" content="staff,ANU,FEIT,faculty,engineering,IT,information,technology,computer,science,australian,national,university,student,future,phd,graduate,postgrad">
  <meta name="author" content="webmaster@cs.anu.edu.au">
  <meta name="abstract" content="Dacapo Benchmark regression test results">
  <meta name="revisit-after" content="1 days">
<!-- END DOCUMENT META DATA -->
</head>

<body marginheight="0" marginwidth="0" topmargin="0">
<? if (($searchBar == "1") && ( $p != "1" )){ include ($includespath."searchBar.inc"); } ?>
<? include ($includespath."globalHeader.inc") ?>

<table cellspacing="0" cellpadding="0" border="0" width="100%" summary="Page content layout table">
	<tr>
		<? include ($nav) ?>
		<td width="99%" align="left" height="300" valign="top"><a name="content"></a>
		<div style="padding-left: 15px; padding-right: 10px;">
<?
If (!$blnPrinterFriendly)
{
 ?>
<div class="caption">
    
</div>
<!-- SET BREADCRUMB TRAIL HERE ^ (above) -->
<?
}
?>

<!-- BEGIN DOCUMENT CONTENT -->
<h1>DaCapo Benchmarks</h1>
<h2>Regression test results</h2>
<?
function passfail($pass,$fail,$style) {
  $status = "";
  $total = $pass + $fail;
  if (($fail == 0) && ($pass > 0)) {
    $status = "pass";
  } elseif ($fail > 0 && $pass == 0) {
    $status = "fail";
  } elseif ($fail == 0 && $pass == 0) {
    $status = "";
  } else {
    $status = "unknown";
  }
  if ($total > 0)
    $pct = (int) (100.0*$pass/$total);
  else
    $pct = 0;
  echo "<td align=\"center\" class=\"$status\" style=\"$style\">$pass/$fail($pct%)</td>";
}

$sizes = array("small","default","large");
$lastsize = "large"; // Put a border after this cell
$vms = array();
$handle = opendir(".");
$available = array();
while (false !== ($file = readdir($handle))) {
  if (substr($file,0,8) == "results-")
    $available[] = substr($file,8);
}
closedir($handle);
//$available = scandir(".",1);
rsort($available);               // Sort descending

//
// Read-ahead in summary file to find out full collection of VMs
// and sizes
//
foreach ($available as $result) {
  $sum_file="results-$result/summary.dat";
  if (file_exists($sum_file)) {
    $sum = fopen($sum_file,"r");
    while (!feof($sum)) {
      $line = fgets($sum);
      //echo "<td><pre>$line</pre></td>";
      $feature = strtok($line," ");
      $item = strtok(" ");
      if ($feature == "size" && !in_array($item,$sizes))
        $sizes[] = $item;
      if ($feature == "vm" && !in_array($item,$vms))
        $vms[] = $item;
    }
  } 
}
//
// Print table header
//
$cols = 1 + count($sizes) + count($vms);
$width = 70/$cols;
echo "<table columns=\"$cols\" style=\"border-collapse:collapse;border: 2px solid black\">\n";
echo "<tr style=\"border: 2px solid black;\">\n";
echo "  <th width=\"$width%\" style=\"border-right: 1px solid gray\">Date</th>\n";
foreach ($sizes as $size) {
  $style="";
  if ($size == $lastsize)
    $style .= "border-right: 1px solid grey;";
  echo "  <th align=\"center\" width=\"$width%\" style=\"$style\">$size</th>\n";
}
foreach ($vms as $vm) 
  echo "  <th align=\"center\" width=\"$width%\">$vm</th>\n";
echo "</tr>\n";
//
// Print one row per result directory
//
foreach ($available as $result) {
  echo "<tr>\n";
  echo "  <td align=\"center\" style=\"border-right: 1px solid gray\">\n";
  echo "      <a href=\"results-$result/\">$result</a></td>\n";
  $sum_file="results-$result/summary.dat";
  if (file_exists($sum_file)) {
    $sum = fopen($sum_file,"r");
    $passes = array();
    $fails = array();
    while (!feof($sum)) {
      $line = fgets($sum);
      $feature = strtok($line," ");
      $item = strtok(" ");
      $passes[$item] = 0 + strtok(" ");
      $fails[$item] = 0 + strtok(" ");
    }
    foreach ($sizes as $size) {
      $style="";
      if ($size == $lastsize)
        $style .= "border-right: 1px solid grey;";
      passfail($passes[$size],$fails[$size],$style);
    }
    $style="border-left: 1px solid grey;";
    foreach ($vms as $vm) {
      passfail($passes[$vm],$fails[$vm],$style);
      $style="";
    }
  } else {
    $span = $cols-1;
    echo "  <td align=\"center\" colspan=\"($span)\">-- No summary file --</td>\n";
  }
  echo "</tr>\n";
}
?>
</table>
<!-- END DOCUMENT CONTENT -->
			</div>
		</td>
	</tr>
</table>
<? include ($includespath."globalFooter.inc"); ?>
</body>
</html>
