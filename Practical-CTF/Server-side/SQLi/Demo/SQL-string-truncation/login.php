<?php
// Simple vulnerable login demo

$users = ['admin' => 'password123'];

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $username = $_POST['username'] ?? '';
    $password = $_POST['password'] ?? '';
    
    // BUG: Truncate tại byte >= 0x80
    $username = preg_replace('/[\x80-\xFF].*/', '', $username);
    
    if (isset($users[$username])) {
        if ($users[$username] === $password) {
            die("✅ Login success! FLAG{character_truncation}");
        } else {
            die("❌ Invalid password");
        }
    } else {
        die("❌ Username not found");
    }
}
?>
<!DOCTYPE html>
<html>
<head><title>Login Demo</title></head>
<body>
<h2>Vulnerable Login</h2>
<form method="POST">
    Username: <input name="username"><br>
    Password: <input name="password" type="password"><br>
    <button>Login</button>
</form>
<hr>
<p>Valid: admin:password123</p>
<p>Try: <code>admin\x80test</code> → becomes "admin"</p>
</body>
</html>