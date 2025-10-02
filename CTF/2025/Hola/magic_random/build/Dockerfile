FROM ghcr.io/n1ght-f4ll/ehctf/python:alpine

RUN mkdir -p /app && \
    pip install --no-cache-dir -i https://pypi.org/simple flask && \
    adduser -D -G www-data -s /bin/ash -h /app www-data

COPY --chmod=500 --chown=www-data:www-data app.py  /app/
COPY templates/ /app/templates/
COPY static/ /app/static/
COPY --chmod=500 --chown=www-data:www-data init.sh /etc/init.sh
RUN sed -i 's/\r//' /etc/init.sh

WORKDIR /app
USER www-data

ENTRYPOINT ["/bin/sh", "/etc/init.sh"]
