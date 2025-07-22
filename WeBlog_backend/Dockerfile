# ======================================
# Production Dockerfile for WeBlog Backend
# ======================================
# Multi-stage build for optimized production image
# Usage: docker build -t weblog-backend .

# ======================================
# Stage 1: Build Application
# ======================================
FROM eclipse-temurin:17-jdk-alpine AS builder

# Set working directory
WORKDIR /app

# Copy Gradle wrapper and configuration files
COPY gradle/ gradle/
COPY gradlew build.gradle settings.gradle ./

# Set executable permissions for gradlew
RUN chmod +x gradlew

# Download dependencies (separate layer for caching)
RUN ./gradlew dependencies --no-daemon

# Copy source code
COPY src/ src/

# Build application
RUN ./gradlew build --no-daemon -x test

# ======================================
# Stage 2: Runtime Image
# ======================================
FROM eclipse-temurin:17-jre-alpine AS runtime

# Install curl for health checks
RUN apk add --no-cache curl

# Create non-root user for security
RUN addgroup -g 1001 weblog && \
    adduser -D -u 1001 -G weblog weblog

# Set working directory
WORKDIR /app

# Copy built application from builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

# Change ownership to weblog user
RUN chown -R weblog:weblog /app

# Switch to non-root user
USER weblog

# ======================================
# Environment Configuration
# ======================================
# Default environment variables (can be overridden)
ENV ENV=production
ENV SERVER_PORT=8080
ENV SPRING_PROFILES_ACTIVE=production

# JVM optimization for container
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:MaxMetaspaceSize=128m"

# ======================================
# Health Check
# ======================================
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:${SERVER_PORT}/actuator/health || exit 1

# ======================================
# Application Startup
# ======================================
EXPOSE ${SERVER_PORT}

# Use exec form to handle signals properly
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]