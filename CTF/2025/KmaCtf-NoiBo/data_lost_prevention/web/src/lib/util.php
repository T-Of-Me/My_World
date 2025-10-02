<?php
declare(strict_types=1);

if (session_status() !== PHP_SESSION_ACTIVE) {
    session_start();
}

@ini_set('display_errors', '0');


if (!isset($_SESSION['uid'])) {
    $_SESSION['uid'] = 1;
    $_SESSION['username'] = 'demo';
}

function h($s) { return htmlspecialchars((string)$s, ENT_QUOTES, 'UTF-8'); }