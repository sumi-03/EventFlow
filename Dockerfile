# 빌드: 소스 -> 실행 가능한 jar
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app
COPY . .
RUN --mount=type=cache,target=/root/.gradle ./gradlew bootJar -x test

# 실행: JRE + jar 만
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
RUN useradd -r -u 1001 appuser
USER appuser
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
