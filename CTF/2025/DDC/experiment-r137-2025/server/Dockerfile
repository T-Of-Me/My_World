FROM openjdk:17-jdk-slim

WORKDIR /app
COPY ./*.jar /app/app.jar

USER www-data

CMD ["java", "-jar", "/app/app.jar"]