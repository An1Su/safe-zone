FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Install curl for healthcheck
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Copy pre-built JAR from Jenkins build
COPY target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
