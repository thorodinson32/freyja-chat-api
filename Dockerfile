############## BUILD STAGE ##############
FROM gradle:8.7-jdk21 as builder
WORKDIR /app

# Copy everything needed for build
COPY build.gradle settings.gradle ./
COPY gradle ./gradle
COPY src ./src

# Build the JAR (skip tests for fastest builds)
RUN gradle bootJar --no-daemon -x test


############## RUNTIME STAGE ##############
FROM eclipse-temurin:21-jdk
WORKDIR /app

# Copy JAR from builder
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]