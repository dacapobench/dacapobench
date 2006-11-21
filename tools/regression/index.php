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
TABLE { border-collapse: collapse; }
*#SIZE { border-right: 2px solid black; }
TR { border=none; }
TR.heading { border: 2px solid black; }
TD { border: 1px solid white; 
     text-align: center;
}
TD.pass { background-color: lightgreen; }
TD.nolog { background-color: white; }
TD.unknown { background-color: yellow; }
TD.p100 { background-color: rgb(80,224,80); }
TD.p95 { background-color: rgb(128,224,80); }
TD.p90 { background-color: rgb(186,224,80); }
TD.p80 { background-color: rgb(224,234,80); }
TD.p60 { background-color: rgb(224,205,80); }
TD.p40 { background-color: rgb(224,186,80); }
TD.p20 { background-color: rgb(224,166,80); }
TD.p1 { background-color: rgb(224,147,80); }
TD.p0 { background-color: rgb(224,116,80); }
TD.fail { background-color: rgb(224,80,80); }
TD.none { background-color: white; }
COL#last { border-right: 2px solid black; }
</style>
<!-- BEGIN DOCUMENT META DATA -->
  <meta name="keywords" content="staff,ANU,FEIT,faculty,engineering,IT,information,technology,computer,science,australian,national,university,student,future,phd,graduate,postgrad">
  <meta name="author" content="webmaster@cs.anu.edu.au">
  <meta name="abstract" content="Dacapo Benchmark regression test results">
  <meta name="revisit-after" content="1 days">
<!-- END DOCUMENT META DATA -->
</head>

<body>


<!-- BEGIN DOCUMENT CONTENT -->
<h1>DaCapo Benchmarks</h1>
<h2>Regression test results</h2>
<?
function passfail($pass,$fail,$style) {
  $status = "";
  $total = $pass + $fail;
  if ($total > 0)
    $pct = (int) (100.0*$pass/$total);
  else
    $pct = 0;
  $bands = array(100,95,90,80,60,40,20,0);
  if ($total == 0) {
    $cellClass = "none";
  } else {
    $cellClass = "fail";
    foreach ($bands as $band) {
      if ($pct >= $band) {
        $cellClass = "p" . $band ;
        break;
      }
    }
  }
  echo "<td align=\"center\" class=\"$cellClass\" style=\"$style\">";
  if ($total > 0) {
    echo "$pass/$total($pct%)";
  } else {
    echo "&nbsp;";
  }
  echo "</td>";
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
foreach ($vms as $vm) {
  echo "  <th align=\"center\" width=\"$width%\">";
  echo "<a href=\"results-$available[0]/$vm/version.txt\">$vm</a>";
  echo "</th>\n";
}
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
echo "</table>";
?>
<h2>Performance tests</h2>
<table>
</table>
</body>
</html>
