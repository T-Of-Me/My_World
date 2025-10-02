FROM openjdk:17-jdk-slim

RUN apt-get update && \
    apt-get install -y curl && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

ARG FLAG
RUN echo $FLAG > /$(openssl rand -hex 6)-flag.txt

WORKDIR /app
COPY ./*.jar /app/app.jar

USER www-data

CMD ["java", "-jar", "/app/app.jar"]