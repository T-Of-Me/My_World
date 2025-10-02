<?php

// Prevent direct access - must come through index.php
if (!defined('SECURE_ACCESS')) {
    die('Direct access not allowed. Please use /?page=home');
}

require 'database.php';
ini_set('display_errors', '1'); 
header('Content-Type: text/html; charset=UTF-8');

$info = unserialize(base64_decode($_COOKIE['user']), ['allowed_classes' => ['User', 'LogFile']]);
$username = $info->username;
$email = $info->email;
$description = $info->description;
$phone = $info->phone;

$conn = new Database();

if (!$conn->checkExist($username)) {
    header('Location: /?page=login');
    exit;
}


$avatar = $conn->getInfo($username)['url_avatar'];
$avatar = $avatar === 'default.png' ? 'upload/default.png' : 'upload/' . $username . '/' . $avatar;


?>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>User Homepage</title>
    <style>
        @import url('https://fonts.googleapis.com/css2?family=Poppins:wght@300;400;500;600;700&display=swap');
        body {
            font-family: Poppins, sans-serif;
            margin: 0;
            padding: 0;
            background-color: #f9f9f9;
            display: flex;
            flex-direction: column;
            align-items: center;
            height: 100vh;
        }
        .profile-container {
            background: #fff;
            border: 1px solid #ddd;
            border-radius: 10px;
            padding: 20px;
            text-align: center;
            width: 300px;
            box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
            margin-top: 20px;
        }
        .avatar {
            width: 100px;
            height: 100px;
            border-radius: 50%;
            object-fit: cover;
            border: 2px solid #ddd;
            margin-bottom: 15px;
        }
        .nickname {
            font-size: 1.2em;
            font-weight: bold;
            margin: 5px 0;
        }
        .email {
            font-size: 0.9em;
            color: #666;
            margin: 5px 0;
        }
        .description, .phone {
            font-size: 0.9em;
            color: #666;
            margin: 5px 0;
        }
        .upload-btn {
	    display: inline-block;
	    padding: 10px 15px;
	    background-color: #007bff;
	    color: white;
	    font-size: 14px;
	    border-radius: 5px;
	    text-align: center;
	    cursor: pointer;
	    border: none;
	}
	.upload-btn:hover {
	    background-color: #0056b3;
	}
        input[type="file"] {
            display: none;
        }
        .info-container {
            background: #fff;
            border: 1px solid #ddd;
            border-radius: 10px;
            padding: 20px;
            width: 300px;
            box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
            margin-top: 20px;
        }
        .taskbar {
            background-color: #007BFF;
            color: white;
            width: 100%;
            padding: 10px;
            
        }
        .taskbar a {
            color: white;
            text-decoration: none;
            margin: 0 10px;
        }
        .taskbar a:hover {
            text-decoration: underline;
        }
    </style>
</head>
<body>
    <div class="taskbar">
        <a href='/?page=view_avatars'>Your Avatars</a>
        <a href='/?page=logout'>Log Out</a>
    </div>

    <div class="profile-container">
    <img src="<?php echo $avatar ?>" alt="Avatar" class="avatar" id="user-avatar">
    <div class="nickname" id="user-nickname"><?php echo $username; ?></div>
    
	    <form action="/?page=upload" method="POST" enctype="multipart/form-data" id="upload-form">
		<label class="upload-btn">
		Upload Avatar
		<input type="file" id="upload-avatar" name="avatar" accept="image/*" onchange="submitForm()" style="display: none;">
		</label>
	    </form>
	</div>

    <div class="info-container">
        <div class="email" id="info-email">Email: <?php echo $email; ?></div>
        <div class="description" id="info-description">Description: <?php echo $description; ?></div>
        <div class="phone" id="info-phone">Phone: <?php echo $phone; ?></div>
        <div class="response" id="upload-response"></div>
    </div>
    
    <script>
	    function submitForm() {
		    document.getElementById('upload-form').submit();
	    }

	</script>
</body>
</html>
