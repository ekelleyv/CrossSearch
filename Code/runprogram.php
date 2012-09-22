<?php
if (isset($_POST['query'])) {
    $file = $_POST['query'];
    exec('java -jar ~/Dropbox/cos435/Search/dist/search.jar '. $file, $output);
    exec('java -jar ~/Dropbox/cos435/Analyze/Analyze/dist/Analyze.jar ' . $file, $output2);
    foreach($output2 as $value) {
        $value = str_replace("<wordle>","\n" , $value);
        print $value;
    }
} 
?>