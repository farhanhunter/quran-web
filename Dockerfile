# ===========================================
# Multi-stage Dockerfile untuk Production
# ===========================================

# Stage 1: Build aplikasi
FROM maven:3.9-eclipse-temurin-17-alpine AS builder

WORKDIR /app

# Copy pom.xml dulu untuk cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code dan build
COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Runtime image
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Install timezone data dan font Arabic
RUN apk add --no-cache \
    tzdata \
    fontconfig \
    ttf-dejavu \
    && fc-cache -f

# Create non-root user untuk security
RUN addgroup -g 1001 -S quran && \
    adduser -u 1001 -S quran -G quran

# Copy jar dari builder stage
COPY --from=builder /app/target/*.jar app.jar

# Create log directory
RUN mkdir -p /var/log/quran-web && \
    chown -R quran:quran /var/log/quran-web

# Switch to non-root user
USER quran

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Set timezone
ENV TZ=Asia/Jakarta

# JVM Options
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# Run application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]