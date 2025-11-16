#!/bin/bash
set -e

# Ensure log directory exists
mkdir -p /var/log/apache2
touch /var/log/apache2/error.log

# Start Apache in background
apachectl -D FOREGROUND &
APACHE_PID=$!

# Wait a moment for Apache to start and create log file
sleep 1

chown -R www-data:www-data /var/www/html/uploads \
    && chmod -R 755 /var/www/html/uploads

# Tail error log to stderr in background
tail -f /var/log/apache2/error.log >&2 &
TAIL_PID=$!

# Function to cleanup on exit
cleanup() {
    kill $APACHE_PID $TAIL_PID 2>/dev/null || true
    exit 0
}

# Trap signals
trap cleanup SIGTERM SIGINT

# Wait for Apache process
wait $APACHE_PID

