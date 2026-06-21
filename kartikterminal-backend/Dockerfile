# Build stage
FROM maven:3.9.5-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Run stage
FROM eclipse-temurin:17-jdk-jammy

# Install polyglot compilers and runtimes required by the backend sandbox
RUN apt-get update && apt-get install -y \
    python3 \
    python3-pip \
    gcc \
    g++ \
    nodejs \
    npm \
    golang-go \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY --from=build /app/target/terminal-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
