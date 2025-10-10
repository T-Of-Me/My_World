<?php
declare(strict_types=1);
require __DIR__ . '/../lib/util.php';
require __DIR__ . '/../lib/db.php';

header('Content-Type: application/json');

$q = $_GET['q'] ?? '';

$q2 = preg_replace('/\s+/u', '', $q); # xóa khoảng trắng  
$q2 = preg_replace('/\b(?:or|and)\b/i', '', $q2); # xóa or, and
$q2 = str_ireplace(["union","load_file","outfile","="], '', $q2); # xóa union, load_file, outfile, =
$filtered = $q2;

if (strlen($filtered) > 90) {
    echo json_encode(['ok' => false]);
    exit;
}

$sql = "SELECT id,title FROM cases WHERE title RLIKE '.*$filtered' AND owner_id = :uid LIMIT 1";

$stmt = $pdo->prepare($sql);
$stmt->execute([':uid' => $_SESSION['uid'] ?? 1]);
$row = $stmt->fetch();

usleep(random_int(1500, 5000));

echo json_encode(['ok' => (bool)$row]);