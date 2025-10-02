FROM ghcr.io/n1ght-f4ll/holactf-2025/php:7.4-alpine

USER root

COPY ./challenge /var/www/html
COPY ./flag.txt /flag.txt

RUN mkdir -p /opt/database

RUN chown -R www-data:www-data /var/www/html && chmod -R 755 /var/www/html
RUN chmod 777 /opt/database

COPY --chmod=500 init.sh /etc/init.sh

ENTRYPOINT [ "/bin/sh" ]
CMD ["/etc/init.sh"]
