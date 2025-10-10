<?php
declare(strict_types=1);

$base = '/var/log/app/';

$file = $_GET['file'] ?? 'app.log';

while (str_contains($file, '../')) {
    $file = str_replace('../', '', $file);
}

if (!preg_match('/\.(log|txt)$/i', $file)) {
    http_response_code(400);
    exit('bad ext');
}

$file2 = urldecode($file);
$path  = $base . $file2;

$real = realpath($path);
if ($real !== false && str_starts_with($real, $base)) {
    @readfile($real);
    exit;
}

if (str_starts_with($path, $base)) {
    @readfile($path);
} else {
    http_response_code(403);
    echo 'forbidden';
}