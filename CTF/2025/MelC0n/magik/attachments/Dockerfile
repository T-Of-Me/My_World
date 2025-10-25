FROM php:8.2


RUN apt update && apt install imagemagick build-essential -y

# READ FLAG
COPY readflag.c /readflag.c
RUN gcc /readflag.c -o /readflag
RUN chmod u+s /readflag
RUN rm -f /readflag.c

COPY /flags.txt /flag.txt
RUN chmod 400 /flag.txt

# CHALLENGE
WORKDIR /app
COPY app/ /app

COPY convert.sh /opt/convert.sh
RUN chmod +x /opt/convert.sh

RUN chown -R www-data:www-data /app


USER www-data
CMD ["php", "-S", "0.0.0.0:8000", "-t", "/app"]


