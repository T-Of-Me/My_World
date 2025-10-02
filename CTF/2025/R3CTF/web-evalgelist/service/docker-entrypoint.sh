#!/bin/sh

# Set up the flag
if [ "$FLAG" ]; then
    echo "$FLAG" > /flag
    chmod 744 /flag
    echo "Flag set: $FLAG"
else
    echo "flag{TEST_Dynamic_FLAG}" > /flag
    chmod 744 /flag
    echo "Default flag set"
fi

# Create log directories
mkdir -p /var/log/nginx
touch /var/log/nginx/access.log /var/log/nginx/error.log

# Start PHP-FPM in background
echo "Starting PHP-FPM..."
php-fpm -D

# Start nginx in foreground
echo "Starting nginx..."
exec nginx -g "daemon off;"