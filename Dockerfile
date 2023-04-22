FROM maven:3.8-openjdk-18-slim AS build
WORKDIR /app
COPY pom.xml .
COPY src/ ./src/
RUN mvn package -DskipTests

FROM gcr.io/distroless/java17-debian11:nonroot
WORKDIR /app
COPY --from=build /app/target/*.jar /app/app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]