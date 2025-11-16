<?php
function generateTraceId() {
    $b = random_bytes(16);
    $b[6] = chr(ord($b[6]) & 0x0f | 0x40);
    $b[8] = chr(ord($b[8]) & 0x3f | 0x80);
    return vsprintf("%s%s-%s-%s-%s-%s%s%s", str_split(bin2hex($b), 4));
}

function generateRequestId() {
    $chars = "0123456789ABCDEFGHIJKLMNOPQRSTUV";
    $requestId = "0x";
    for ($i = 0; $i < 44; $i++) {
        $requestId .= $chars[random_int(0, strlen($chars) - 1)];
    }
    return $requestId;
}

function validateTraceId($traceId) {
    if (!preg_match("/([0-9a-f-]+)/", $traceId)) {
        return false;
    }
    if (strlen($traceId) !== 36) {
        return false;
    }
    if (substr_count($traceId, "-") !== 4) {
        return false;
    }
    return true;
}

function validateRequestId($requestId) {
    if (strpos($requestId, "0x") !== 0) {
        return false;
    }
    if (!preg_match("/([0-9A-U]+)/", $requestId)) {
        return false;
    }
    if (strlen($requestId) !== 46) {
        return false;
    }
    return true;
}
?>
