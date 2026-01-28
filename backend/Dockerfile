# Use a multi-stage build to create a lean image

# Stage 1: Build the application
FROM eclipse-temurin:17-jdk-jammy as builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
COPY mvnw ./mvnw
RUN --mount=type=cache,target=/root/.m2 ./mvnw package -Dmaven.test.skip=true

# Stage 2: Create the final image
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
# Get the JAR file from the builder stage
COPY --from=builder /app/target/*.jar app.jar
# Expose the port your Spring Boot app runs on (default is 8080)
EXPOSE 8080
# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
