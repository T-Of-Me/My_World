<?php


if(isset($_FILES['img']) && isset($_POST['name'])) {
    $proc = proc_open(
        $cmd = [
            '/opt/convert.sh',
            $_FILES['img']['tmp_name'],
            $outputName = 'static/'.$_POST['name'].'.png'
        ],
        [],
        $pipes
    );
    proc_close($proc);

} else {
    highlight_file(__FILE__);
}


