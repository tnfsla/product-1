# Use a multi-stage build to create a lean image

# Stage 1: Build the application
FROM eclipse-temurin:17-jdk-jammy as builder
WORKDIR /app
# Install Maven
RUN apt-get update && apt-get install -y maven
COPY pom.xml .
COPY src ./src
# Clear specific dependency cache before resolving
RUN rm -rf /root/.m2/repository/com/google/cloud/google-cloud-firestore/1.36.0/
RUN --mount=type=cache,target=/root/.m2 mvn dependency:resolve --update-snapshots
RUN --mount=type=cache,target=/root/.m2 mvn package -Dmaven.test.skip=true --update-snapshots

# Stage 2: Create the final image
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
# Get the JAR file from the builder stage
COPY --from=builder /app/target/*.jar app.jar
# Expose the port your Spring Boot app runs on (default is 8080)
EXPOSE 8080
# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
