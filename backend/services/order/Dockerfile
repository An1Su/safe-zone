# Build stage
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build

# Copy and build shared module
COPY shared /build/shared
WORKDIR /build/shared
RUN mvn clean install -DskipTests -B

# Build service
WORKDIR /build/service
COPY services/order/pom.xml .
COPY services/order/src ./src
RUN mvn clean package -DskipTests -B

# Runtime stage
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Install curl for healthcheck
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

COPY --from=builder /build/service/target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
