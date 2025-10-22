# Single-stage: build & run a single-file Java app (no Maven/Gradle)
FROM eclipse-temurin:21-jdk
WORKDIR /app


# Copy source and compile once at build time
COPY App.java .
RUN javac App.java


# Default port + remote debug port
ENV PORT=8080
EXPOSE 8080 5005


# Enable JDWP for remote debugging on port 5005
CMD ["sh", "-c", "java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005 App"]