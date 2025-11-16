<?php
ini_set("display_errors", 0);
error_reporting(0);

require_once "utils.php";

define("GEMINAI_SALT", "[**REDACTED**]");

// Configuration
$uploadDir = __DIR__ . "/uploads";
$fontFile = __DIR__ . "/assets/CascadiaCode.ttf";
$logoPath = __DIR__ . "/assets/logo.png";

$maxFileSize = 1 * 1024 * 1024; // 5 MB

$fontSize = 14;
$padding  = 20;

$maxWidth  = 2048;
$maxHeight = 2048;
$minWidth  = 100;
$minHeight = 100;

// Set content type to JSON
header("Content-Type: application/json");

// Font setup
if (!file_exists($fontFile)) {
    echo json_encode([
        "success" => false,
        "error" => "Font file 'CascadiaCode.ttf' not found."
    ]);
    exit;
}

try {
    if ($_SERVER["REQUEST_METHOD"] !== "POST") {
        echo json_encode([
            "success" => false,
            "error" => "Method not allowed"
        ]);
        exit;
    }

    $imageData = file_get_contents("php://input");
    if ($imageData === "") {
        echo json_encode([
            "success" => false,
            "error" => "Invalid input data."
        ]);
        exit;
    }

    $traceId = $_SERVER["HTTP_X_GEMINAI_TRACE_ID"] ?? "";
    $requestId = $_SERVER["HTTP_X_GEMINAI_REQUEST_ID"] ?? "";
    $modelName = $_SERVER["HTTP_X_GEMINAI_MODEL"] ?? "";
    $startTime = (int)$_SERVER["HTTP_X_GEMINAI_START_TIME"] ?? round(microtime(true) * 1000);

    error_log("[PROCESS-IMAGE] Trace ID: $traceId");
    error_log("[PROCESS-IMAGE] Request ID: $requestId");
    error_log("[PROCESS-IMAGE] Model Name: $modelName");
    error_log("[PROCESS-IMAGE] Start Time: $startTime");

    if ($traceId === "" || $requestId === "" || $modelName === "") {
        echo json_encode([
            "success" => false,
            "error" => "Invalid input data."
        ]);
        exit;
    }

    $parts = [$requestId, $traceId, time(), GEMINAI_SALT];
    $imageId = sha1(implode("|", $parts));
    $imagePath = "$uploadDir/request-$imageId.png";

    error_log("[PROCESS-IMAGE] Image ID: $imageId");
    error_log("[PROCESS-IMAGE] Image Path: $imagePath");
    error_log("[PROCESS-IMAGE] Image Data Length: " . strlen($imageData) . " bytes");

    $num_bytes = file_put_contents($imagePath, $imageData);
    if ($num_bytes === false) {
        echo json_encode([
            "success" => false,
            "error" => "Failed to upload image file."
        ]);
        exit;
    }

    // Get image info
    [$width, $height, $type] = getimagesize($imagePath);
    if (!$width || !$height) {
        echo json_encode([
            "success" => false,
            "error" => "Invalid image file."
        ]);
        exit;
    }

    if ($type !== IMAGETYPE_PNG) {
        echo json_encode([
            "success" => false,
            "error" => "Only PNG files are supported."
        ]);
        exit;
    }

    if ($width < $minWidth || $height < $minHeight) {
        echo json_encode([
            "success" => false,
            "error" => "Image too small."
        ]);
        exit;
    }

    if ($width > $maxWidth || $height > $maxHeight) {
        echo json_encode([
            "success" => false,
            "error" => "Image too large."
        ]);
        exit;
    };

    // Create image resource
    $image = imagecreatefrompng($imagePath);
    imagesavealpha($image, true);

    // Convert to grayscale
    if (!imagefilter($image, IMG_FILTER_GRAYSCALE)) {
        echo json_encode([
            "success" => false,
            "error" => "Failed to apply grayscale filter."
        ]);
        exit;
    }

    $watermark = "Generated using the $modelName model";

    // Calculate text box height
    $bbox = imagettfbbox($fontSize, 0, $fontFile, $watermark);
    $textHeight = abs($bbox[5] - $bbox[1]) + $padding * 2;

    // Create new image with extra space for text
    $newHeight = $height + $textHeight;
    $newImage  = imagecreatetruecolor($width, $newHeight);
    imagesavealpha($newImage, true);

    $transparent = imagecolorallocatealpha($newImage, 0, 0, 0, 127);
    imagefill($newImage, 0, 0, $transparent);

    $black = imagecolorallocate($newImage, 0, 0, 0);
    imagefilledrectangle($newImage, 0, $height, $width, $newHeight, $black);

    // Copy original image
    imagecopy($newImage, $image, 0, 0, 0, 0, $width, $height);

    // Add logo (bottom-right corner)
    if (file_exists($logoPath)) {
        $logo = imagecreatefrompng($logoPath);
        imagesavealpha($logo, true);
        $logoWidth = imagesx($logo);
        $logoHeight = imagesy($logo);
        $dstX = $width - $logoWidth - 10;
        $dstY = $newHeight - $textHeight - $logoHeight - 10;
        imagecopy($newImage, $logo, $dstX, $dstY, 0, 0, $logoWidth, $logoHeight);
        imagedestroy($logo);
    } else {
        // Skip if logo not found
    }

    // Draw text
    $white = imagecolorallocate($newImage, 255, 255, 255);
    $textX = $padding;
    $textY = $height + $padding + abs($bbox[5]);
    imagettftext($newImage, $fontSize, 0, $textX, $textY, $white, $fontFile, $watermark);

    // Caculate time consume
    $totalTime = round(microtime(true) * 1000) - $startTime;

    // Get image data
    ob_start();                     // Start output buffering
    imagepng($newImage);            // Output image to buffer
    $generatedImageData = ob_get_clean();    // Get buffer content

    $outputPath = "$uploadDir/result-$imageId.png";
    file_put_contents($outputPath, $generatedImageData);

    error_log("[PROCESS-IMAGE] Output Image Path: $outputPath");
    error_log("[PROCESS-IMAGE] Output Image Data Length: " . strlen($generatedImageData) . " bytes");
    error_log("[PROCESS-IMAGE] Total time: $totalTime ms" );

    echo json_encode([
        "success" => true,
        "originalImage" => $imagePath,
        "generatedImage" => $outputPath,
        "totalTime" => $totalTime
    ]);
} catch (Exception $e) {
    error_log("[PROCESS-IMAGE] Failed to generate image: " . $e->getMessage());
    echo json_encode([
        "success" => false,
        "error" => $e->getMessage()
    ]);
}
?>
