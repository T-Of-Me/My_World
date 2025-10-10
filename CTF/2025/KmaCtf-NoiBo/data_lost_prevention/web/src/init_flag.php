<?php
declare(strict_types=1);
require __DIR__ . '/lib/db.php';

function uuidv4(): string {
    $data = random_bytes(16);
    $data[6] = chr((ord($data[6]) & 0x0f) | 0x40);
    $data[8] = chr((ord($data[8]) & 0x3f) | 0x80);
    $hex = bin2hex($data);
    return sprintf('%s-%s-%s-%s-%s',
        substr($hex,0,8), substr($hex,8,4), substr($hex,12,4), substr($hex,16,4), substr($hex,20,12));
}


$chk = $pdo->query("SELECT COUNT(*) AS c FROM attachments WHERE is_lost=1");
$has = $chk ? (int)$chk->fetch()['c'] : 0;
if ($has > 0) {
    exit(0);
}

$uuid = uuidv4();
$dir  = '/var/data/flags';
@mkdir($dir, 0755, true);
$flagPath = $dir . '/flag-' . $uuid . '.txt';
$flagVal  = "KMACTF{hehe}\n";
file_put_contents($flagPath, $flagVal);


$stmt = $pdo->prepare("INSERT INTO attachments(case_id, filename, storage_path, is_lost) VALUES (1, ?, ?, 1)");
$stmt->execute(['Q2-incident-raw.csv', $flagPath]);

exit(0);