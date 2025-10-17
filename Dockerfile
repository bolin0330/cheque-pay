# Use Maven to build the project first
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests

# Use a lighter JDK image to run the jar
FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app

# Copy the built jar from the builder stage
COPY --from=builder /app/target/*.jar app.jar

# Set environment variables (Render will automatically inject environment parameters)
ENV JAVA_OPTS="-Xms256m -Xmx512m"

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]