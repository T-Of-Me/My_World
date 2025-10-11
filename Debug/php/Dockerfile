FROM php:8.2-apache

# Thư mục làm việc của Apache + PHP
WORKDIR /var/www/html

# Bật mod_rewrite (tuỳ chọn, hữu ích cho router/.htaccess)
RUN a2enmod rewrite

# (Không COPY code ở đây vì sẽ mount ./src vào runtime bằng docker-compose)
# Nếu muốn build sẵn code vào image, bỏ comment dòng dưới:
# COPY src/ /var/www/html/
RUN pecl install xdebug
RUN docker-php-ext-enable xdebug
COPY ./config/docker-php-ext-xdebug.ini /usr/local/etc/php/conf.d/docker-php-ext-xdebug.ini
