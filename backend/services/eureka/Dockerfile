# Build stage
FROM eclipse-temurin:17-jdk-jammy AS builder
WORKDIR /build

# Copy Maven wrapper and parent POM
COPY mvnw mvnw.cmd pom.xml ./
COPY services/eureka ./services/eureka

# Make mvnw executable
RUN chmod +x mvnw

# Build Eureka server
WORKDIR /build/services/eureka
RUN ../../mvnw clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app

# Install curl for healthcheck
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

COPY --from=builder /build/services/eureka/target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
