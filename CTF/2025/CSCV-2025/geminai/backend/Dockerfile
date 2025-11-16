# Base image
FROM ubuntu:20.04

# Disable interactive prompt
ENV DEBIAN_FRONTEND=noninteractive
ENV TZ=Asia/Ho_Chi_Minh

# Install Apache, PHP, and necessary extensions
RUN apt-get update && \
    apt-get install -y apache2 php libapache2-mod-php php-curl php-gd php-pgsql && \
    apt-get clean && rm -rf /var/lib/apt/lists/*

# Enable Apache mod_rewrite and mod_reqtimeout
RUN a2enmod rewrite reqtimeout

# Configure Apache to allow .htaccess (if you want rewrite to work in /var/www/html)
RUN echo "ServerName backend" >> /etc/apache2/apache2.conf
RUN sed -i '/<Directory \/var\/www\/>/,/<\/Directory>/ s/AllowOverride None/AllowOverride All/' /etc/apache2/apache2.conf

# Copy Apache timeout configuration
COPY apache-timeout.conf /etc/apache2/conf-available/timeout.conf
RUN a2enconf timeout

# Configure PHP error_log to stderr so errors appear in Docker logs
RUN echo "error_log = /proc/self/fd/2" >> /etc/php/7.4/apache2/php.ini
RUN echo "max_input_vars = 31337" >> /etc/php/7.4/apache2/php.ini

# Copy PHP files to Apache document root
COPY *.php /var/www/html/
COPY .htaccess /var/www/html/
COPY assets /var/www/html/assets/

# Set proper permissions
RUN chown -R www-data:www-data /var/www/html \
    && chmod -R 755 /var/www/html

# Expose port 80 (Apache default)
EXPOSE 80

# Copy startup script
COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

# Start Apache with error log tailing
CMD ["/entrypoint.sh"]
