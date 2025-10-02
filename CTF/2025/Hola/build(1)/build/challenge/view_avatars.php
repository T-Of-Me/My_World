<?php 

// Prevent direct access - must come through index.php
if (!defined('SECURE_ACCESS')) {
    die('Direct access not allowed. Please use /?page=view_avatars');
}


require 'LogAndCheck.php';
require 'database.php';
ini_set('display_errors', '0'); 

$info = unserialize(base64_decode($_COOKIE['user']), ['allowed_classes' => ['User', 'LogFile']]);
$username = $info->username;

$conn = new Database();
if (!$conn->checkExist($username)) {
    header('Location: /?page=login');
    exit;
}

$avatars = $conn->getAllAvatars($username);
var_dump($avatars);

?>


<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Uploaded Files</title>
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
            min-height: 100vh;
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
        .gallery-container {
            margin-top: 20px;
            background: #fff;
            padding: 20px;
            border-radius: 10px;
            box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
            width: 80%;
            max-width: 900px;
        }
        .gallery-container h2 {
            text-align: center;
            margin-bottom: 20px;
        }
        .gallery {
            display: grid;
            grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
            gap: 15px;
        }
        .gallery img {
            width: 100%;
            height: 150px;
            object-fit: cover;
            border-radius: 10px;
            box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
        }
        
        .preview-container {
	    display: none;
	    position: fixed;
	    top: 0;
	    left: 0;
	    width: 100vw;
	    height: 100vh;
	    background: rgba(0, 0, 0, 0.8);
	    display: flex;
	    justify-content: center;
	    align-items: center;
	    z-index: 1000;
	}

	.preview-container img {
	    max-width: 90vw;
	    max-height: 90vh;
	    border-radius: 10px;
	}

	.preview-container.active {
	    display: flex;
	}
    </style>
</head>
<body>
    <div class="taskbar">
        <a href='/?page=home'>Home</a>
        <a href='/?page=logout'>Log Out</a>
    </div>


    <div class="gallery-container">
        <h2>Uploaded Files </h2>
        <div class="gallery">
            <?php 
                foreach ($avatars as $avatar) {
                    var_dump($avatars);

                    $avatar_path = "upload/$username/". $avatar['image'];
                    var_dump($avatar_path);

                    echo("ssss");
                    $image_data = file_get_contents($avatar_path);
                    if ($image_data !== false) {
                        echo("mmm");
                        $image_info = getimagesize($avatar_path);
                        $mime_type = $image_info['mime'] ?? 'image/png';
                        $avatar_base64 = 'data:' . $mime_type . ';base64,' . base64_encode($image_data);
                    }
                    echo "<img src='" . htmlspecialchars($avatar_base64) . "' alt='Avatar' class='avatar' onclick=\"showPreview(this.src)\">";
                }
                
            ?>
        </div>
    </div>
</body>
</html>
