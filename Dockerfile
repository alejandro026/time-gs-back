# Stage 1: Build the application
FROM eclipse-temurin:22-jdk-alpine AS builder
WORKDIR /app

# Copy Maven wrapper and pom.xml files
COPY .mvn/ .mvn
COPY mvnw pom.xml ./

# Install execution permission for maven wrapper script
RUN chmod +x mvnw

# Resolve dependencies (offline mode for caching layer optimization)
RUN ./mvnw dependency:go-offline

# Copy source code and build the final jar file
COPY src ./src
RUN ./mvnw clean package -DskipTests

# Stage 2: Create a lightweight runtime image
FROM eclipse-temurin:22-jre-alpine
WORKDIR /app

# Copy the generated JAR from the builder stage
COPY --from=builder /app/target/*.jar app.jar

# Expose port
EXPOSE 8080

# Execute the application
ENTRYPOINT ["java", "-jar", "app.jar"]
