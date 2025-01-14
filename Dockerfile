# Use an official OpenJDK runtime as a parent image
FROM openjdk:17-jdk-slim

# Set the working directory in the container
WORKDIR /app

# Copy the fat JAR file into the container
COPY build/libs/confession-bot-1.0.0.jar app.jar

# Copy the .env file to the container (optional, if needed)
COPY .env .env

# Expose ports (optional, only if your app needs it, e.g., for HTTP endpoints)
# EXPOSE 8080

# Set the environment variable for the bot token (useful for CI/CD environments)
# ENV BOT_TOKEN=your-token-placeholder

# Command to run the JAR file
CMD ["java", "-jar", "app.jar"]