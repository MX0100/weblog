# Use official OpenJDK 17 base image
FROM openjdk:17-jdk-slim

# Install necessary tools
RUN apt-get update && apt-get install -y \
    curl \
    wget \
    && rm -rf /var/lib/apt/lists/*

# Set working directory
WORKDIR /app

# Copy Gradle files
COPY gradle/ gradle/
COPY gradlew .
COPY gradlew.bat .
COPY build.gradle .
COPY settings.gradle .

# Set executable permissions
RUN chmod +x ./gradlew

# Pre-download dependencies (optional, speeds up subsequent builds)
RUN ./gradlew dependencies --no-daemon || true

# Copy source code in production environment
#COPY src/ src/

# Set DevTools environment variables for hot reload
ENV SPRING_DEVTOOLS_RESTART_ENABLED=true
ENV SPRING_DEVTOOLS_LIVERELOAD_ENABLED=true
ENV SPRING_DEVTOOLS_RESTART_POLL_INTERVAL=1000
ENV SPRING_DEVTOOLS_RESTART_QUIET_PERIOD=400

# Set JVM options for better DevTools performance in Docker
ENV JAVA_OPTS="-Dspring.devtools.restart.poll-interval=1000 -Dspring.devtools.restart.quiet-period=400"

# Expose ports
EXPOSE 8080
EXPOSE 35729

# Use bootRun to start application with hot reload support
CMD ["./gradlew", "bootRun", "--no-daemon"]