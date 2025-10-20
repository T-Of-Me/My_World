FROM openjdk:17-jdk-slim

WORKDIR /app

COPY portfolio.war portfolio.war
COPY flag.txt /flag.txt

RUN groupadd -r webgroup && \
    useradd -r -g webgroup -m -d /home/web web && \
    mkdir -p /app/data && \
    chown -R web:webgroup /app && \
    chmod 555 /app/portfolio.war && \
    chmod 770 /app/data && chown web:webgroup /app/data && \
    RAND_NAME=$(tr -dc A-Za-z0-9 </dev/urandom | head -c 16) && \
    mv /flag.txt "/${RAND_NAME}" && \
    rm -f /flag.txt && \
    chown root:root "/${RAND_NAME}" && chmod 444 "/${RAND_NAME}"

USER web

EXPOSE 8989

ENTRYPOINT ["java", "-jar", "/app/portfolio.war"]
