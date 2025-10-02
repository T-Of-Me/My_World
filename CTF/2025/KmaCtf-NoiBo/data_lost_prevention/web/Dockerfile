FROM php:8.2-apache

RUN docker-php-ext-install pdo pdo_mysql

RUN { \
      echo "display_errors=0"; \
      echo "log_errors=1"; \
      echo "error_log=/var/log/php-error.log"; \
    } > /usr/local/etc/php/conf.d/ctf.ini

COPY start.sh /start.sh
RUN chmod +x /start.sh

ENV APACHE_DOCUMENT_ROOT /var/www/html
RUN sed -ri -e 's!/var/www/html!${APACHE_DOCUMENT_ROOT}!g' /etc/apache2/sites-available/000-default.conf

CMD ["/start.sh"]
