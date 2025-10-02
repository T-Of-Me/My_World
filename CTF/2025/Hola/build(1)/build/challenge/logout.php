<?php
// Unset custom cookies (if any exist)
if (isset($_COOKIE['user'])) {
    setcookie('user', '', time() - 3600, '/');
}

header("Location: /?page=login");
exit();
?>