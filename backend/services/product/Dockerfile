# Build stage
FROM eclipse-temurin:17-jdk-jammy AS builder
WORKDIR /build

# Copy Maven wrapper and parent POM
COPY mvnw mvnw.cmd pom.xml ./
COPY shared ./shared
COPY services/product ./services/product

# Make mvnw executable
RUN chmod +x mvnw

# Build shared module first
WORKDIR /build
RUN ./mvnw clean install -DskipTests -f pom.xml -pl shared -am

# Build product service
WORKDIR /build/services/product
RUN ../../mvnw clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app

COPY --from=builder /build/services/product/target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
