FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .

RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN apk add --no-cache wget
COPY --from=build /app/target/*.jar app.jar

HEALTHCHECK --interval=30s --timeout=3s CMD wget -q -O /dev/null http://localhost:6969/ || exit 1

ENV SPRING_PROFILES_ACTIVE=dev
ENV JAVA_OPTS=""

EXPOSE 6969
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
