<?php

class LogFile
{
    public $filename;
    
    public function __destruct()
    {
        return md5_file($this->filename); // hash rồi phar://
    }
}

class Logger
{   
    public function __destruct()
    {
        $request_log = fopen($this->logs , "a");
        fwrite($request_log, $this->request);
        fwrite($request_log, "\r\n");
        fclose($request_log);
    }
}



function checkMd5AndLog($md5Hash)  // lưu hash 
{
    if (strlen($md5Hash) !== 32 || !ctype_xdigit($md5Hash)) {
        return;
    }
    $file = 'logMD5.php';

    if (!file_exists($file)) {
        touch($file);
    }
   
    $entry = $md5Hash . PHP_EOL;
    file_put_contents($file, $entry, FILE_APPEND);
}
