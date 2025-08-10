# Use an official OpenJDK runtime as a parent image
FROM openjdk:21-jdk-slim

# Set the working directory
WORKDIR /app

# Copy Gradle wrapper and build files
COPY gradlew build.gradle settings.gradle ./
COPY gradle ./gradle

# Copy source code
COPY src ./src

# Make Gradle wrapper executable
RUN chmod +x gradlew

# Build the application
RUN ./gradlew bootJar --no-daemon

# Copy the built jar to the container
RUN cp build/libs/*.jar app.jar

# Expose port 8080
EXPOSE 8081

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
