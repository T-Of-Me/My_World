<?php 

// Prevent direct access - must come through index.php
if (!defined('SECURE_ACCESS')) {
    die('Direct access not allowed. Please use /?page=register');
}

require "database.php";
if ($_SERVER["REQUEST_METHOD"] == "POST") {
    
    if (isset($_POST["username"], $_POST["password"]) && 
    !empty($_POST["username"]) && 
    !empty($_POST["password"])) {
        $username = $_POST["username"];
        $password = $_POST["password"];
        $register_check = true;
        // Validate username: only alphanumeric characters (a-zA-Z0-9)
        if (!preg_match('/^[a-zA-Z0-9]+$/', $username) || strlen($username) < 3 || strlen($username) > 15) {
            $message = "Choose another username!";
            $register_check = false;
        }

        $username = htmlspecialchars(strip_tags($username));
        $password = htmlspecialchars(strip_tags($password));
        $phone = '0';
        for ($i = 0; $i < 9; $i++) {
            $phone .= random_int(0, 9);
        }
        $email = $username . "@gmail.com";

        $adjectives = array(
            "Crazy", "Mystic", "Silent", "Ancient", "Ghost", "Rogue", "Sly", "Swift", "Cyber", "Shadow",
            "Fierce", "Brave", "Dark", "Electric", "Wild", "Vicious", "Savage", "Fearless", "Noble", "Arcane",
            "Cryptic", "Lethal", "Mighty", "Vigilant", "Eternal", "Infernal", "Galactic", "Astral", "Spectral",
            "Stealthy", "Wicked", "Enigmatic", "Daring", "Merciless", "Grim", "Majestic", "Phantom", "Elusive", "Ferocious"
        );
        $nouns = array(
            "Hacker", "Coder", "Pirate", "Ninja", "Agent", "Phantom", "Wizard", "Knight", "Guru", "Sniper",
            "Samurai", "Mercenary", "Vanguard", "Warden", "Assassin", "Hunter", "Guardian", "Sentinel", "Ranger", "Warrior",
            "Seeker", "Champion", "Conqueror", "Defender", "Destroyer", "Invader", "Marauder", "Raider", "Vanquisher", "Warlord",
            "Sorcerer", "Mage", "Shaman", "Druid", "Paladin", "Necromancer", "Berserker", "Monk", "Priest", "Rogue"
        );
        
        $description = $adjectives[random_int(0, sizeof($adjectives) - 1)] . " " . $nouns[random_int(0, sizeof($nouns) - 1)];
        $conn = new Database();
        
        if (!$register_check) {
            $message = "Registration failed due to invalid username";
        } else if ($conn->checkExist($username)) {
            $message = "Registration failed due to duplicate information";
        } else {
            if ($conn->register($username, $password, $phone, $email, $description)) {
                header("Location: /?page=login");
            } else {
                $message = "Registration failed due to server error";
            }
        }
    } else {
        $message = "Registration failed due to missing information";
    }
}
?>

<!DOCTYPE html>
<html lang="en" dir="ltr">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title> Registration </title>
    <style>
        @import url('https://fonts.googleapis.com/css?family=Poppins:400,500,600,700&display=swap');
        *{
        margin: 0;
        padding: 0;
        box-sizing: border-box;
        font-family: 'Poppins', sans-serif;
        }
        body{
        min-height: 100vh;
        display: flex;
        align-items: center;
        justify-content: center;
        background: #4070f4;
        }
        .wrapper{
        position: relative;
        max-width: 430px;
        width: 100%;
        background: #fff;
        padding: 34px;
        border-radius: 6px;
        box-shadow: 0 5px 10px rgba(0,0,0,0.2);
        }
        .wrapper h2{
        position: relative;
        font-size: 22px;
        font-weight: 600;
        color: #333;
        }
        .wrapper h2::before{
        content: '';
        position: absolute;
        left: 0;
        bottom: 0;
        height: 3px;
        width: 28px;
        border-radius: 12px;
        background: #4070f4;
        }
        .wrapper form{
        margin-top: 30px;
        }
        .wrapper form .input-box{
        height: 52px;
        margin: 18px 0;
        }
        form .input-box input{
        height: 100%;
        width: 100%;
        outline: none;
        padding: 0 15px;
        font-size: 17px;
        font-weight: 400;
        color: #333;
        border: 1.5px solid #C7BEBE;
        border-bottom-width: 2.5px;
        border-radius: 6px;
        transition: all 0.3s ease;
        }
        .input-box input:focus,
        .input-box input:valid{
        border-color: #4070f4;
        }
        form .role{
        display: flex;
        align-items: normal;
        }
        form h3{
        color: #707070;
        font-size: 14px;
        font-weight: 500;
        margin-left: 10px;
        margin-right: 10px;
        }
        .input-box.button input{
        color: #fff;
        letter-spacing: 1px;
        border: none;
        background: #4070f4;
        cursor: pointer;
        }
        .input-box.button input:hover{
        background: #0e4bf1;
        }
        form .text h3{
        color: #333;
        width: 100%;
        text-align: center;
        }
        form .text h3 a{
        color: #4070f4;
        text-decoration: none;
        }
        form .text h3 a:hover{
        text-decoration: underline;
        }
    </style>
</head>
<body>
    <div class="wrapper">
        <h2>Register Form</h2>

        <form action="" method="POST">
            <div class="input-box">
                <input type="text" placeholder="Enter your username" id="username" name="username" required>
            </div>

            <div class="input-box">
                <input type="password" placeholder="Enter your password" id="password" name="password" required>
            </div>
            
            <div class="input-box button">
                <input type="Submit" value="Register Now">
            </div>
            <div class="text">
                <h3>Already have an account? <a href="/?page=login">Login now</a></h3>
            </div>

            <div class="text">
                <h3><?php if (isset($message)) { echo $message; } ?></h3>
            </div>
        </form>
            
        
    </div>

</body>
</html>
