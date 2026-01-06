# Build stage
FROM eclipse-temurin:17-jdk-jammy AS builder
WORKDIR /build

# Copy Maven wrapper and parent POM
COPY mvnw mvnw.cmd pom.xml ./
COPY shared ./shared
COPY services/media ./services/media

# Make mvnw executable
RUN chmod +x mvnw

# Build shared module first
WORKDIR /build
RUN ./mvnw clean install -DskipTests -f pom.xml -pl shared -am

# Build media service
WORKDIR /build/services/media
RUN ../../mvnw clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app

COPY --from=builder /build/services/media/target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
