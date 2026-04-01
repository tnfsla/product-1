# Use a multi-stage build to create a lean image

# Stage 1: Build the application
FROM eclipse-temurin:17-jdk-jammy as builder
WORKDIR /app
# Install Maven
RUN apt-get update && apt-get install -y maven wget
COPY pom.xml .
COPY src ./src
RUN mvn dependency:resolve --update-snapshots
RUN mvn package -Dmaven.test.skip=true --update-snapshots

# Stage 2: Create the final image
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
RUN apt-get update && \
    apt-get install -y tesseract-ocr tesseract-ocr-kor tesseract-ocr-eng && \
    apt-get clean && rm -rf /var/lib/apt/lists/*
# Get the JAR file from the builder stage
COPY --from=builder /app/target/*.jar app.jar
# Expose the port your Spring Boot app runs on (default is 8080)
EXPOSE 8080
# Run the application
ENTRYPOINT ["java", "-Xmx400m", "-jar", "app.jar"]
