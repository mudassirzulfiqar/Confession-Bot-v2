# Stage 1: Build the application
FROM gradle:7.6-jdk17 AS builder

# Set the working directory inside the container
WORKDIR /app

# Copy Gradle build files
COPY build.gradle.kts settings.gradle.kts gradlew ./
COPY gradle gradle

# Copy the source code
COPY src src

# Build the application
RUN ./gradlew shadowJar --no-daemon

# Stage 2: Run the application
FROM openjdk:17-jdk-slim

# Set the working directory inside the container
WORKDIR /app

# Copy the fat JAR from the builder stage
COPY --from=builder /app/build/libs/confession-bot-1.0.0.jar app.jar

# Expose the port (optional, only if needed)
# EXPOSE 8080

# Command to run the application
CMD ["java", "-jar", "app.jar"]