if [ -n "$GZCTF_FLAG" ]; then
    echo "$GZCTF_FLAG" > /flag.txt 2>/dev/null || true && \
    chmod 444 /flag.txt 2>/dev/null || true && \
    unset GZCTF_FLAG 2>/dev/null || true
fi && \

rm -f /usr/local/apache2/logs/httpd.pid
php-fpm -D
nginx -g 'daemon off;'