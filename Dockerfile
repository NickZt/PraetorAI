FROM openjdk:21-jdk-slim

LABEL maintainer="nickzt@tactorder.com"
LABEL description="Research Decision Support System (RDSS)"

# Install required packages
RUN apt-get update && apt-get install -y \
    curl \
    wget \
    git \
    python3 \
    python3-pip \
    && rm -rf /var/lib/apt/lists/*

# Set working directory
WORKDIR /app

# Copy Gradle wrapper and build files
COPY build.gradle.kts settings.gradle.kts gradlew ./
COPY gradle ./gradle

# Make gradlew executable
RUN chmod +x gradlew

# Download dependencies
RUN ./gradlew dependencies --configuration runtimeClasspath

# Copy source code
COPY src ./src

# Copy resources
COPY src/main/resources ./src/main/resources

# Build the application
RUN ./gradlew bootJar -x test

# Expose port
EXPOSE 8080

# Create non-root user
RUN groupadd -r rdss && useradd -r -g rdss rdss
RUN chown -R rdss:rdss /app
USER rdss

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run the application
CMD ["java", "-jar", "build/libs/rdss-0.1.0.jar"]
