<?php
declare(strict_types=1);

$DB_HOST = getenv('DB_HOST') ?: 'db';
$DB_NAME = getenv('DB_NAME') ?: 'casetrack';
$DB_USER = getenv('DB_USER') ?: 'ctf';
$DB_PASS = getenv('DB_PASS') ?: 'ctfpass';

$options = [
  PDO::ATTR_ERRMODE => PDO::ERRMODE_SILENT,
  PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
  PDO::ATTR_EMULATE_PREPARES => true,
];

try {
    $pdo = new PDO("mysql:host=$DB_HOST;dbname=$DB_NAME;charset=utf8mb4", $DB_USER, $DB_PASS, $options);
} catch (Throwable $e) {
    http_response_code(500);
    exit('DB down');
}
