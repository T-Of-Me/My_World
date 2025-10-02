<?php
// Prevent direct access - must come through index.php
if (!defined('SECURE_ACCESS')) {
  die('Direct access not allowed. Please use /?page=upload');
}

require 'LogAndCheck.php';
require 'database.php';
ini_set('display_errors', '1'); 
header('Content-Type: text/html; charset=UTF-8');

$info = unserialize(base64_decode($_COOKIE['user']), ['allowed_classes' => ['User', 'LogFile']]);
$username = $info->username;
if ($username == null) {
  header('Location: /?page=login');
}

$conn = new Database();

$target_dir = "upload/$username/";
if (!is_dir($target_dir)) {
  mkdir($target_dir, 0777, true);
}

$target_file = $target_dir . basename($_FILES["avatar"]["name"]);

if (file_exists($target_file)) {
  echo "Sorry, file already exists.";
  exit;
}

$allowedExtensions = ['jpg', 'jpeg', 'png', 'gif'];
$fileName = $_FILES['avatar']['name'];


$fileExt = strtolower(end(explode('.', $fileName)));

if (!in_array($fileExt, $allowedExtensions)) {
  echo "Sorry, your file type is not allowed.";
  exit;
}

if ($_FILES["avatar"]["size"] > 5000000) {
  echo "Sorry, your file is too large.";
  exit;
}


if (move_uploaded_file($_FILES["avatar"]["tmp_name"], $target_file)) {
  echo "$target_file has been uploaded.";
  $getMD5 = md5_file($target_file); //  $getMD5 = hash  phar://upload/testq/avatar.gif/a' 
  $md5Hash = (string)$getMD5;
  checkMd5AndLog($getMD5);
  $conn->updateAvatar($username, $fileName);
  header('Location: /?page=home');
}

