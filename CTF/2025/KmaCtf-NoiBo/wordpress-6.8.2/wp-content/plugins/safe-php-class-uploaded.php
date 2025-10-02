<?php
/*
Plugin Name: Safe PHP Class Upload (read-only, non-executable)
Description: A plugin to safely upload PHP class files for review, without executing them. Files are stored as .txt to prevent execution.
Version: 0.1
Author: meulody
*/

if (!defined('ABSPATH')) {
    exit;
}

class Safe_Class_Uploader
{
    const SAFE_UPLOAD_DIR = 'uploads_safe_classes';
    const MAX_FILE_SIZE = 64;

    public function __construct()
    {
        add_action('rest_api_init', function () {
            register_rest_route('safe-upload/v1', '/upload', array(
                'methods' => 'POST',
                'callback' => array($this, 'handle_upload'),
                'permission_callback' => '__return_true'
            ));
        });
    }

    private function ensure_dir()
    {
        $dir = self::SAFE_UPLOAD_DIR;
        if (!is_dir($dir)) {
            if (!@mkdir($dir, 0750, true)) {
                return new WP_Error('dir_error', 'Could not create safe storage directory on server.', array('status' => 500));
            }
        }
        return true;
    }

    private function has_dangerous_tokens($content)
    {
        $dangerous = array(
            'exit(',
            'die(',
            'file(',
            'echo(',
            'print(',
            'printf(',
            'print_r(',
            'var_dump(',
            'var_export(',
            'debug_zval_dump(',
            'encode(',
            'decode(',
            'exec(',
            'system(',
            'shell_exec(',
            'passthru(',
            'proc_open(',
            'eval(',
            'assert(',
            '`',
            'contents(',
            'open(',
            'bin2hex(',
            'serialize(',
            'htmlspecialchars(',
            'htmlentities(',
            'unlink(',
            'rename(',
            'goto ',
            'new ',
            'copy ('
        );
        $hay = strtolower($content);
        foreach ($dangerous as $tok) {
            if (strpos($hay, $tok) !== false)
                return $tok;
        }
        return false;
    }

    public function handle_upload(\WP_REST_Request $request)
    {
        $ok = $this->ensure_dir();
        if (is_wp_error($ok))
            return $ok;

        $files = $request->get_file_params();
        if (empty($files['file'])) {
            return new WP_Error('no_file', 'Could not find upload file (field name = file).', array('status' => 400));
        }
        $file = $files['file'];

        if ($file['size'] > self::MAX_FILE_SIZE) {
            return new WP_Error('too_big', 'File is too large.', array('status' => 400));
        }

        $content = file_get_contents($file['tmp_name']);
        if ($content === false) {
            return new WP_Error('read_error', 'Could not read temporary file.', array('status' => 500));
        }

        if (!preg_match('/class\s+[A-Za-z0-9_]+/i', $content)) {
            return new WP_Error('no_class', 'File does not contain a valid class declaration.', array('status' => 400));
        }

        $bad = $this->has_dangerous_tokens($content);
        if ($bad !== false) {
            return new WP_Error('dangerous_code', 'File contains dangerous token: ' . $bad, array('status' => 400));
        }

        $filename = pathinfo($file['name'], PATHINFO_FILENAME) . '.txt';
        $fullpath = rtrim(self::SAFE_UPLOAD_DIR, '/') . '/' . $filename;

        if (file_put_contents($fullpath, $content) === false) {
            return new WP_Error('write_err', 'Could not save the safe file.', array('status' => 500));
        }

        @chmod($fullpath, 0644);

        return rest_ensure_response(array(
            'status' => 'ok',
            'message' => 'Upload successful.',
        ));
    }
}

new Safe_Class_Uploader();
