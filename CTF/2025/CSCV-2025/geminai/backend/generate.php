<?php
ini_set("display_errors", 0);
error_reporting(0);

require_once "config.php";
require_once "utils.php";

define("PROCESS_IMAGE_URL", "http://backend/process-image");
define("MAX_UPLOAD_SIZE", 1 * 1024 * 1024); // 1MB

// Set content type to JSON
header("Content-Type: application/json");

try {
    if ($_SERVER["REQUEST_METHOD"] !== "POST") {
        echo json_encode([
            "success" => false,
            "error" => "Method not allowed"
        ]);
        exit;
    }

    // Get form data
    $requestId = $_POST["requestId"] ?? generateRequestId();
    $traceId = $_POST["traceId"] ?? generateTraceId();
    $modelId = $_POST["modelId"] ?? 0;
    $userId = $_POST["userId"] ?? 0;
    $image = $_FILES["image"];

    error_log("[BACKEND] Generate Request - Trace ID: $traceId");
    error_log("[BACKEND] Generate Request - Request ID: $requestId");
    error_log("[BACKEND] Generate Request - Model ID: $modelId");
    error_log("[BACKEND] Generate Request - User ID: $userId");

    if (!validateTraceId($traceId)) {
        error_log("[BACKEND] Invalid Trace ID: $traceId");
        echo json_encode([
            "success" => false,
            "traceId" => $traceId,
            "error" => "Invalid Trace ID."
        ]);
        exit;
    }

    if (!validateRequestId($requestId)) {
        error_log("[BACKEND] Invalid Request ID: $requestId");
        echo json_encode([
            "success" => false,
            "requestId" => $requestId,
            "error" => "Invalid Request ID."
        ]);
        exit;
    }

    if ($modelId === "" || $userId === "" || strlen($modelId) > 32 || strlen($userId) > 32) {
        echo json_encode([
            "success" => false,
            "error" => "Invalid input data."
        ]);
        exit;
    }

    // $modelId is integer so that it is safe from SQL injection
    $sql = "SELECT id, name, available FROM ai_models WHERE id = $modelId";
    $stmt = $pdo->query($sql);
    $model = $stmt->fetch(PDO::FETCH_ASSOC);

    if (!$model) {
        error_log("[BACKEND] Model not found: $modelId");
        echo json_encode([
            "success" => false,
            "error" => "Model not found"
        ]);
        exit;
    }

    if ($model["available"] == false) {
        error_log("[BACKEND] Model is currently disabled and cannot be use: $modelId");
        echo json_encode([
            "success" => false,
            "error" => "Model is currently disabled and cannot be use"
        ]);
        exit;
    }

    // $userId is integer so that it is safe from SQL injection
    $sql = "SELECT id, username, email FROM users WHERE id = $userId";
    $stmt = $pdo->query($sql);
    $user = $stmt->fetch(PDO::FETCH_ASSOC);
    if (!$user) {
        error_log("[BACKEND] User not found: $userId");
        echo json_encode([
            "success" => false,
            "error" => "User not found"
        ]);
        exit;
    }
    
    // Logs all generate image requests
    $stmt = $pdo->prepare("
        INSERT INTO generated_logs (user_id, request_id, trace_id, model_id, model_name)
        VALUES (?, ?, ?, ?, ?)
        ON CONFLICT (request_id) DO NOTHING
    ");
    $stmt->execute([$userId, $requestId, $traceId, $modelId, $model["name"]]);

    if (!isset($_FILES["image"]) || $_FILES["image"]["error"] !== UPLOAD_ERR_OK) {
        error_log("[BACKEND] Failed to upload image: $requestId");
        echo json_encode([
            "success" => false,
            "error" => "Failed to upload image."
        ]);
        exit;
    }

    // Prevent user from upload large file
    $size = $_FILES["image"]["size"];
    if ($size > MAX_UPLOAD_SIZE) {
        error_log("[BACKEND] Image too large: $requestId Size: $size bytes");
        echo json_encode([
            "success" => false,
            "error" => "Image too large."
        ]);
        exit;
    }

    // Generate image processing request
    $imagePath = $_FILES["image"]["tmp_name"];
    $imageData = file_get_contents($imagePath);
    $modelName = $model["name"];

    $curl = curl_init();

    curl_setopt($curl, CURLOPT_HTTPHEADER, array(
        "Content-Type: application/octet-stream",
        "X-Geminai-Trace-Id: $traceId",
        "X-Geminai-Request-Id: $requestId",
        "X-Geminai-Start-Time: " . round(microtime(true) * 1000),
        "X-Geminai-Model: $modelName",
        "X-Geminai-Version: 1.3.37",
    ));

    curl_setopt_array($curl, array(
        CURLOPT_RETURNTRANSFER => 1,
        CURLOPT_URL => PROCESS_IMAGE_URL,
        CURLOPT_USERAGENT => "Secure Genminai/1.3.37",
        CURLOPT_POST => 1,
    ));

    curl_setopt($curl, CURLOPT_POSTFIELDS, $imageData);

    $data = curl_exec($curl);

    $res = json_decode($data, 1);

    curl_close($curl);

    if ($res["success"] === true) {
        // Remove the absolute path to prevent leaking server file structure
        $originalImage = str_replace(getcwd(), "", $res["originalImage"]);
        $generatedImage = str_replace(getcwd(), "", $res["generatedImage"]);
        $totalTime = $res["totalTime"];

        // Update log with image paths
        $stmt = $pdo->prepare("UPDATE generated_logs SET original_image = ?, generated_image = ?, total_time = ? WHERE request_id = ?");
        $stmt->execute([$originalImage, $generatedImage, $totalTime, $requestId]);

        // Store generated image to database
        $stmt = $pdo->prepare("
            INSERT INTO generated_images (user_id, original_image, generated_image, model_name)
            VALUES (?, ?, ?, ?)
        ");
        $result = $stmt->execute([$userId, $originalImage, $generatedImage, $modelName]);

        if (!$result) {
            error_log("[BACKEND] Failed to insert generated image: $requestId");
            echo json_encode([
                "success" => false,
                "error" => "Failed to insert generated image"
            ]);
        }

        echo json_encode([
            "success" => true,
            "requestId" => $requestId,
            "modelName" => $modelName,
            "originalImage" => $originalImage,
            "generatedImage" => $generatedImage,
            "totalTime" => $totalTime
        ]);
    } else {
        error_log("[BACKEND] Failed to generate image: $requestId " . $res["error"]);
        echo json_encode([
            "success" => false,
            "requestId" => $requestId,
            "error" => $res["error"] ?? "Something went wrong"
        ]);
    }
} catch (Exception $e) {
    error_log("[BACKEND] Failed to generate image: " . $e->getMessage());
    echo json_encode([
        "success" => false,
        "error" => $e->getMessage()
    ]);
}
?>
