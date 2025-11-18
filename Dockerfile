FROM gradle:8.5.0-jdk21 AS builder

WORKDIR /home/gradle/src

COPY build.gradle settings.gradle gradlew ./
COPY gradle gradle
RUN gradle dependencies --no-daemon || true

COPY . .

RUN gradle clean bootJar --no-daemon

FROM eclipse-temurin:21-jdk-jammy AS runtime

WORKDIR /app

RUN mkdir -p /app/newrelic
ADD ./newrelic/newrelic.jar /app/newrelic/newrelic.jar
ADD ./newrelic/newrelic.yml /app/newrelic/newrelic.yml


COPY --from=builder /home/gradle/src/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-javaagent:/app/newrelic/newrelic.jar", "-jar", "app.jar"]