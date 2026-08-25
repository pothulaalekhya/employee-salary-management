# ==============================================================================
# Stage 1: Build JAR using Maven and OpenJDK 17
# ==============================================================================
FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder

WORKDIR /build

# Cache dependencies first
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and package application (skipping unit tests for fast Docker build)
COPY src ./src
RUN mvn clean package -DskipTests -B

# ==============================================================================
# Stage 2: Minimal JRE 17 Alpine Runtime Image
# ==============================================================================
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Create non-root system user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy compiled executable JAR from builder stage
COPY --from=builder /build/target/employee-salary-backend-1.0.0.jar /app/app.jar

# Set ownership to unprivileged user
RUN chown -R appuser:appgroup /app

USER appuser

# Configurable runtime environment variables (overridden by Render / Compose)
ENV CORS_ALLOWED_ORIGINS="http://localhost:4200,http://localhost:80,http://127.0.0.1:4200"

EXPOSE 8081

# Allows appending application arguments such as `--seed` directly
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
