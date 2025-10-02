<?php
// Define a constant to mark that access is through index.php
define('SECURE_ACCESS', true);

$regex="/(secret|proc|environ|access|error|\.\.|\/|,|;|[|]|\|connect)/i";

// Check if page parameter is provided
if(isset($_GET['page']) && !empty($_GET['page']))
{
    // Validate the page parameter against the regex
    if(!preg_match_all($regex,$_GET['page']))
    {
        // Add .php extension if not present
        $page = $_GET['page'];
        
        if(!preg_match('/\.php$/', $page)) {
            $page .= '.php';
        }
        
        // Check if file exists before including
        if(file_exists($page)) {
            include($page);
        } else {
            echo "<tr><center><strong><td><font color='red' size=50 >\nPage not found!\n</font></td>";
            die('<p style="text-align:center;">The requested page does not exist.</p>');
        }
    }
    
} else {
    header("Location: /?page=home"); // Default page if no page parameter is provided
}
