<?php
class Database {
    private $db_path = "/opt/database/database.db";
    private $conn;

    // Kết nối cơ sở dữ liệu SQLite
    public function __construct() {
        $this->conn = null;
        try {
            // Tạo thư mục nếu chưa tồn tại
            $dir = dirname($this->db_path);
            if (!file_exists($dir)) {
                mkdir($dir, 0777, true);
            }
            
            $this->conn = new PDO("sqlite:" . $this->db_path);
            $this->conn->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
            
            // Tạo bảng nếu chưa tồn tại
            $this->initDatabase();
        } catch(PDOException $exception) {
            echo "Connection error: " . $exception->getMessage();
        }
        return $this->conn;
    }
    
    private function initDatabase() {
        $sql = "
        CREATE TABLE IF NOT EXISTS accounts (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            username TEXT NOT NULL UNIQUE,
            password TEXT NOT NULL,
            phone TEXT NOT NULL,
            email TEXT NOT NULL,
            description TEXT NOT NULL,
            url_avatar TEXT NOT NULL DEFAULT 'default.png'
        );
        
        CREATE TABLE IF NOT EXISTS images (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            username TEXT NOT NULL,
            image TEXT NOT NULL
        );
        ";
        
        $this->conn->exec($sql);
    }
    
    public function register($username, $password, $phone, $email, $description, $url_avatar = 'default.png') {
        $query = "INSERT INTO accounts (username, password, phone, email, description, url_avatar) VALUES (:username,:password, :phone, :email, :description, :url_avatar);";
        $stmt = $this->conn->prepare($query);

        $stmt->bindParam(":username", $username);
        $stmt->bindParam(":password", $password);
        $stmt->bindParam(":phone", $phone);
        $stmt->bindParam(":email", $email);
        $stmt->bindParam(":description", $description);
        $stmt->bindParam(":url_avatar", $url_avatar);

        if ($stmt->execute()) {
            return true;
        } else {
            return false;
        }
    }
    
    public function login($username, $password) {
        $query = "SELECT * FROM accounts WHERE username=:username AND password=:password";
        $stmt = $this->conn->prepare($query);

        $stmt->bindParam(":username", $username);
        $stmt->bindParam(":password", $password);

        $stmt->execute();
        $result = $stmt->fetch(PDO::FETCH_ASSOC);

        return $result !== false ? true : false;
    }

    public function getInfo($username) {
        $query = "SELECT * FROM accounts WHERE username=:username";
        $stmt = $this->conn->prepare($query);

        $stmt->bindParam(":username", $username);

        if ($stmt->execute()) {
            $info = $stmt->fetch(PDO::FETCH_ASSOC);
            return $info;
        } else {
            return false;
        }
    }

    public function getAllAvatars($username) {
        $query = "SELECT image FROM images WHERE username=:username";
        $stmt = $this->conn->prepare($query);

        $stmt->bindParam(":username", $username);

        if ($stmt->execute()) {
            $avatars = $stmt->fetchAll(PDO::FETCH_ASSOC);
            return $avatars;
        } else {
            return false;
        }
    }

    public function checkExist($username) {
        $query = "SELECT * FROM accounts WHERE username=:username";
        $stmt = $this->conn->prepare($query);

        $stmt->bindParam(":username", $username);

        $stmt->execute();
        $result = $stmt->fetch(PDO::FETCH_ASSOC);

        return $result !== false ? true : false;
    }
    
    public function updateAvatar($username, $avatar) {
        $query = "INSERT INTO images (username, image) VALUES (:username, :avatar)";
        $stmt = $this->conn->prepare($query);

        $stmt->bindParam(":username", $username);
        $stmt->bindParam(":avatar", $avatar);
        if ($stmt->execute()) {
            $query = "UPDATE accounts SET url_avatar=:avatar WHERE username=:username";
            $stmt = $this->conn->prepare($query);

            $stmt->bindParam(":username", $username);
            $stmt->bindParam(":avatar", $avatar);

            if ($stmt->execute()) {
                return true;
            } else {
                return false;
            }

        } else {
            return false;
        }
    }
}

class User {
    public $username;
    public $phone;
    public $email;
    public $description;
}

?>