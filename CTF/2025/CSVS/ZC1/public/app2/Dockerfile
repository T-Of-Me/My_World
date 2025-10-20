FROM php:apache


RUN apt update -y && apt install 7zip

COPY ./src /var/www/html

RUN chown -R www-data:www-data /var/www/html/storage

RUN pecl install xdebug
RUN docker-php-ext-enable xdebug
COPY ./config/docker-php-ext-xdebug.ini /usr/local/etc/php/conf.d/docker-php-ext-xdebug.ini